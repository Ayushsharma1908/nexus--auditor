package com.nexuscomply.cyber.parser.paloalto;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.UnknownConstruct;
import com.nexuscomply.cyber.parser.VendorParser;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Palo Alto PAN-OS Configuration Parser.
 *
 * <p>Supports both:
 * <ul>
 *   <li>(i) Line-oriented {@code set}-format configuration lines (e.g. {@code set deviceconfig system ...})</li>
 *   <li>(ii) XML running-config export (e.g. {@code <config version="..."><devices><entry name="localhost.localdomain">...})</li>
 * </ul>
 *
 * <p><b>Format note:</b> The hierarchical brace format ({@code deviceconfig { system { ... } }}) is the interactive
 * CLI display format and is NOT supported. Panorama configuration hierarchies are also out of scope.
 * The parser sniffs the format dynamically without prior assumptions.
 *
 * <p><b>XML Security:</b> Parsed securely via StAX (no DTD, no external entities, external access disabled).
 * Source map line numbers are captured via XML locator with raw text mapped to the source line.
 * Malformed XML never throws, reporting errors safely via {@link ParserResult#getUnknowns()}.
 *
 * <p><b>Extraction Rules:</b>
 * <ul>
 *   <li><b>SSH / Telnet / HTTPS:</b> Interface-management profiles attached to interfaces, plus mgmt-interface
 *       service settings. Evaluated in two passes: defined profiles are only considered enabled if attached to
 *       at least one interface. Defined-but-unattached profiles are ignored.</li>
 *   <li><b>Telnet Asymmetry:</b> {@code security.telnet.enabled} is {@code false} ONLY on an affirmative disable
 *       statement (e.g. {@code disable-telnet yes}) with no enabling anywhere; otherwise left UNSET (null).</li>
 *   <li><b>SSH Version:</b> {@code security.ssh.version} is left UNSET unless explicitly configured.</li>
 *   <li><b>SNMP:</b> Community-based configuration (v2c) extracts {@code snmp.enabled = true} with version left
 *       unset (null), following the Juniper convention. SNMPv3 user configuration extracts {@code snmp.enabled = true}
 *       and {@code snmp.version = "3"}.</li>
 *   <li><b>Syslog:</b> Syslog server profile with &ge; 1 server AND referenced from log settings sets
 *       {@code logging.syslog = true}. Unreferenced profiles leave it unset (null).</li>
 *   <li><b>Local Logging:</b> {@code logging.localLogging} is intentionally left UNSET (null) (not inferred from platform behavior).</li>
 *   <li><b>NTP:</b> Configured NTP server address sets {@code ntp.configured = true}.</li>
 *   <li><b>AAA:</b> External authentication profile (RADIUS, TACACS+, LDAP, Kerberos, SAML) that is actually
 *       referenced for administrator authentication sets {@code authentication.aaa = true}. Local-only admins leave AAA unset.</li>
 * </ul>
 */
@Component
public class PaloAltoPanOsParser implements VendorParser {

    // Set CLI Patterns
    private static final Pattern SET_PREFIX_PATTERN = Pattern.compile("(?i)^set\\s+(.+)$");

    // Management interface services: set deviceconfig system service disable-telnet yes
    private static final Pattern SET_MGMT_SERVICE_PATTERN = Pattern.compile(
            "(?i)^set\\s+deviceconfig\\s+system\\s+service\\s+(disable-telnet|disable-ssh|disable-https|disable-http|disable-snmp)\\s+(yes|no)\\s*$"
    );

    // Explicit SSH version: set deviceconfig system ssh-service version 2
    private static final Pattern SET_SSH_VERSION_PATTERN = Pattern.compile(
            "(?i)^set\\s+deviceconfig\\s+system\\s+ssh-service\\s+version\\s+(1|2)\\s*$"
    );

    // Interface management profiles: set network profiles interface-management-profile <profile> (ssh|telnet|https|http|snmp|ping) (yes|no)
    private static final Pattern SET_INTF_MGMT_PROFILE_PATTERN = Pattern.compile(
            "(?i)^set\\s+network\\s+profiles\\s+interface-management-profile\\s+(\\S+)\\s+(ssh|telnet|https|http|snmp|ping)\\s+(yes|no)\\s*$"
    );

    // Interface attachment: set network interface ethernet <intf> layer3 interface-management-profile <profile>
    private static final Pattern SET_INTF_ATTACHMENT_PATTERN = Pattern.compile(
            "(?i)^set\\s+network\\s+interface\\s+\\S+\\s+.*interface-management-profile\\s+(\\S+)\\s*$"
    );

    // SNMP v2c: set deviceconfig system snmp-setting access-setting version v2c snmp-community-string <comm>
    private static final Pattern SET_SNMP_V2C_PATTERN = Pattern.compile(
            "(?i)^set\\s+deviceconfig\\s+system\\s+snmp-setting\\s+access-setting\\s+version\\s+v2c\\s+snmp-community-string\\s+\\S+.*$"
    );

    // SNMP v3: set deviceconfig system snmp-setting access-setting version v3 users <user> ...
    private static final Pattern SET_SNMP_V3_USER_PATTERN = Pattern.compile(
            "(?i)^set\\s+deviceconfig\\s+system\\s+snmp-setting\\s+access-setting\\s+version\\s+v3\\s+users\\s+(\\S+).*$"
    );

    // Syslog profile server: set shared log-settings syslog <profile> server <server-name> server <host>
    // or set deviceconfig system syslog <profile> server <server-name> server <host>
    private static final Pattern SET_SYSLOG_SERVER_PATTERN = Pattern.compile(
            "(?i)^set\\s+(?:shared|deviceconfig\\s+system)\\s+log-settings\\s+syslog\\s+(\\S+)\\s+server\\s+\\S+\\s+server\\s+(\\S+).*$"
    );

    // Syslog forward reference: set shared log-settings (system|config|profiles ...) match-list <name> send-syslog <profile>
    private static final Pattern SET_SYSLOG_REF_PATTERN = Pattern.compile(
            "(?i)^set\\s+(?:shared|deviceconfig\\s+system)\\s+log-settings\\s+.*\\bsend-syslog\\s+(\\S+).*$"
    );

    // NTP server: set deviceconfig system ntp-servers (primary-ntp-server|secondary-ntp-server) ntp-server-address <address>
    private static final Pattern SET_NTP_SERVER_PATTERN = Pattern.compile(
            "(?i)^set\\s+deviceconfig\\s+system\\s+ntp-servers\\s+(?:primary-ntp-server|secondary-ntp-server)\\s+ntp-server-address\\s+(\\S+).*$"
    );

    // AAA external profile definition: set shared authentication-profile <profile> method (radius|tacplus|ldap|kerberos|saml)
    private static final Pattern SET_AUTH_PROFILE_PATTERN = Pattern.compile(
            "(?i)^set\\s+(?:shared\\s+)?authentication-profile\\s+(\\S+)\\s+method\\s+(radius|tacplus|ldap|kerberos|saml)\\b.*$"
    );

    // AAA admin reference: set deviceconfig system authentication-profile <profile>
    // or set mgt-config users <admin> authentication-profile <profile>
    private static final Pattern SET_ADMIN_AUTH_REF_PATTERN = Pattern.compile(
            "(?i)^set\\s+(?:deviceconfig\\s+system|mgt-config\\s+users\\s+\\S+)\\s+authentication-profile\\s+(\\S+).*$"
    );

    @Override
    public boolean supports(String vendor, String platform) {
        String v = vendor != null ? vendor.trim().toUpperCase() : "";
        String p = platform != null ? platform.trim().toUpperCase() : "";

        if (v.contains("CISCO") || v.contains("JUNIPER") || v.contains("FORTI")) {
            return false;
        }
        if (p.contains("IOS") || p.contains("JUNOS") || p.contains("FORTI")) {
            return false;
        }

        boolean vendorMatches = v.contains("PALO") || v.contains("PAN-OS") || v.contains("PANOS");
        boolean platformMatches = p.contains("PAN-OS") || p.contains("PANOS");

        return vendorMatches || platformMatches;
    }

    @Override
    public ParserResult parse(String rawConfig) {
        ParserResult result = new ParserResult();

        if (rawConfig == null || rawConfig.trim().isEmpty()) {
            result.setStatus("FAILED");
            result.getUnknowns().add(new UnknownConstruct("", 0, "Configuration content is empty or null"));
            return result;
        }

        String trimmed = rawConfig.trim();
        if (isBraceFormat(rawConfig)) {
            result.setStatus("FAILED");
            result.setCanonical(null);
            result.getUnknowns().add(new UnknownConstruct(
                    rawConfig.lines().filter(l -> !l.trim().isEmpty()).findFirst().orElse(""),
                    1,
                    "Hierarchical brace-format PAN-OS configuration is not supported (only 'set' CLI format and XML export format are supported)"
            ));
            return result;
        }

        if (trimmed.startsWith("<")) {
            return parseXml(rawConfig, result);
        } else {
            return parseSet(rawConfig, result);
        }
    }

    private boolean isBraceFormat(String config) {
        String trimmed = config.trim();
        return !trimmed.startsWith("<") && config.contains("{") && config.contains("}");
    }

    // =========================================================================
    // PARSER STATE CONTAINER (Shared by Set and XML parsers)
    // =========================================================================

    private static class ParseContext {
        // Management profiles: profileName -> map of service name to boolean (e.g. "ssh" -> true)
        Map<String, Map<String, Boolean>> profileServices = new LinkedHashMap<>();
        Map<String, Map<String, SourceMapEntry>> profileServiceSources = new LinkedHashMap<>();

        // Attached profiles: set of profile names attached to an interface
        Set<String> attachedProfiles = new LinkedHashSet<>();
        Map<String, SourceMapEntry> attachedProfileSources = new LinkedHashMap<>();

        // Management interface service toggles
        Boolean mgmtDisableTelnet = null;
        SourceMapEntry mgmtDisableTelnetSource = null;

        Boolean mgmtDisableSsh = null;
        SourceMapEntry mgmtDisableSshSource = null;

        Boolean mgmtDisableHttps = null;
        SourceMapEntry mgmtDisableHttpsSource = null;

        Integer explicitSshVersion = null;
        SourceMapEntry explicitSshVersionSource = null;

        // SNMP
        boolean snmpV2cFound = false;
        SourceMapEntry snmpV2cSource = null;

        Set<String> snmpV3Users = new LinkedHashSet<>();
        SourceMapEntry snmpV3Source = null;

        // Syslog profiles: profileName -> set of server hosts
        Map<String, Set<String>> syslogProfileServers = new LinkedHashMap<>();
        Map<String, SourceMapEntry> syslogProfileSources = new LinkedHashMap<>();

        Set<String> referencedSyslogProfiles = new LinkedHashSet<>();
        Map<String, SourceMapEntry> syslogRefSources = new LinkedHashMap<>();

        // NTP
        boolean ntpConfigured = false;
        SourceMapEntry ntpSource = null;

        // AAA
        Set<String> externalAuthProfiles = new LinkedHashSet<>();
        Set<String> adminAuthProfilesUsed = new LinkedHashSet<>();
        Map<String, SourceMapEntry> aaaRefSources = new LinkedHashMap<>();
    }

    // =========================================================================
    // SET FORMAT PARSER
    // =========================================================================

    private ParserResult parseSet(String rawConfig, ParserResult result) {
        ParseContext ctx = new ParseContext();
        String[] lines = rawConfig.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String rawLine = lines[i];
            String trimmed = rawLine.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("/*") || trimmed.startsWith("!")) {
                continue;
            }

            boolean matched = false;

            // 1. Management interface services (disable-telnet, disable-ssh, disable-https)
            Matcher mgmtSvcMatcher = SET_MGMT_SERVICE_PATTERN.matcher(trimmed);
            if (mgmtSvcMatcher.matches()) {
                String svc = mgmtSvcMatcher.group(1).toLowerCase();
                boolean disabled = "yes".equalsIgnoreCase(mgmtSvcMatcher.group(2));
                if ("disable-telnet".equals(svc)) {
                    ctx.mgmtDisableTelnet = disabled;
                    ctx.mgmtDisableTelnetSource = new SourceMapEntry("security.telnet.enabled", lineNum, rawLine);
                } else if ("disable-ssh".equals(svc)) {
                    ctx.mgmtDisableSsh = disabled;
                    ctx.mgmtDisableSshSource = new SourceMapEntry("security.ssh.enabled", lineNum, rawLine);
                } else if ("disable-https".equals(svc)) {
                    ctx.mgmtDisableHttps = disabled;
                    ctx.mgmtDisableHttpsSource = new SourceMapEntry("security.https.enabled", lineNum, rawLine);
                }
                matched = true;
            }

            // 2. Explicit SSH version
            if (!matched) {
                Matcher sshVerMatcher = SET_SSH_VERSION_PATTERN.matcher(trimmed);
                if (sshVerMatcher.matches()) {
                    ctx.explicitSshVersion = Integer.parseInt(sshVerMatcher.group(1));
                    ctx.explicitSshVersionSource = new SourceMapEntry("security.ssh.version", lineNum, rawLine);
                    matched = true;
                }
            }

            // 3. Interface management profile service definition
            if (!matched) {
                Matcher profileMatcher = SET_INTF_MGMT_PROFILE_PATTERN.matcher(trimmed);
                if (profileMatcher.matches()) {
                    String profileName = profileMatcher.group(1);
                    String svcName = profileMatcher.group(2).toLowerCase();
                    boolean enabled = "yes".equalsIgnoreCase(profileMatcher.group(3));

                    ctx.profileServices.computeIfAbsent(profileName, k -> new LinkedHashMap<>()).put(svcName, enabled);
                    String fieldName = mapServiceToCanonicalField(svcName);
                    if (fieldName != null) {
                        ctx.profileServiceSources.computeIfAbsent(profileName, k -> new LinkedHashMap<>())
                                .put(svcName, new SourceMapEntry(fieldName, lineNum, rawLine));
                    }
                    matched = true;
                }
            }

            // 4. Interface attachment
            if (!matched) {
                Matcher intfAttachMatcher = SET_INTF_ATTACHMENT_PATTERN.matcher(trimmed);
                if (intfAttachMatcher.matches()) {
                    String profileName = intfAttachMatcher.group(1);
                    ctx.attachedProfiles.add(profileName);
                    ctx.attachedProfileSources.put(profileName, new SourceMapEntry("interface-management-profile", lineNum, rawLine));
                    matched = true;
                }
            }

            // 5. SNMP v2c
            if (!matched && SET_SNMP_V2C_PATTERN.matcher(trimmed).matches()) {
                ctx.snmpV2cFound = true;
                ctx.snmpV2cSource = new SourceMapEntry("security.snmp.enabled", lineNum, rawLine);
                matched = true;
            }

            // 6. SNMP v3
            if (!matched) {
                Matcher snmpV3Matcher = SET_SNMP_V3_USER_PATTERN.matcher(trimmed);
                if (snmpV3Matcher.matches()) {
                    ctx.snmpV3Users.add(snmpV3Matcher.group(1));
                    ctx.snmpV3Source = new SourceMapEntry("security.snmp.enabled", lineNum, rawLine);
                    matched = true;
                }
            }

            // 7. Syslog server profile definition
            if (!matched) {
                Matcher syslogSrvMatcher = SET_SYSLOG_SERVER_PATTERN.matcher(trimmed);
                if (syslogSrvMatcher.matches()) {
                    String profileName = syslogSrvMatcher.group(1);
                    String host = syslogSrvMatcher.group(2);
                    ctx.syslogProfileServers.computeIfAbsent(profileName, k -> new LinkedHashSet<>()).add(host);
                    ctx.syslogProfileSources.put(profileName, new SourceMapEntry("logging.syslog", lineNum, rawLine));
                    matched = true;
                }
            }

            // 8. Syslog reference in log settings
            if (!matched) {
                Matcher syslogRefMatcher = SET_SYSLOG_REF_PATTERN.matcher(trimmed);
                if (syslogRefMatcher.matches()) {
                    String profileName = syslogRefMatcher.group(1);
                    ctx.referencedSyslogProfiles.add(profileName);
                    ctx.syslogRefSources.put(profileName, new SourceMapEntry("logging.syslog", lineNum, rawLine));
                    matched = true;
                }
            }

            // 9. NTP Server
            if (!matched) {
                Matcher ntpMatcher = SET_NTP_SERVER_PATTERN.matcher(trimmed);
                if (ntpMatcher.matches()) {
                    ctx.ntpConfigured = true;
                    ctx.ntpSource = new SourceMapEntry("ntp.configured", lineNum, rawLine);
                    matched = true;
                }
            }

            // 10. AAA external auth profile definition
            if (!matched) {
                Matcher authProfMatcher = SET_AUTH_PROFILE_PATTERN.matcher(trimmed);
                if (authProfMatcher.matches()) {
                    String profileName = authProfMatcher.group(1);
                    ctx.externalAuthProfiles.add(profileName);
                    matched = true;
                }
            }

            // 11. AAA admin auth profile reference
            if (!matched) {
                Matcher adminAuthMatcher = SET_ADMIN_AUTH_REF_PATTERN.matcher(trimmed);
                if (adminAuthMatcher.matches()) {
                    String profileName = adminAuthMatcher.group(1);
                    ctx.adminAuthProfilesUsed.add(profileName);
                    ctx.aaaRefSources.put(profileName, new SourceMapEntry("authentication.aaa", lineNum, rawLine));
                    matched = true;
                }
            }

            if (!matched) {
                result.getUnknowns().add(new UnknownConstruct(rawLine, lineNum, "Unrecognized or unsupported Palo Alto PAN-OS command"));
            }
        }

        populateCanonicalFromContext(ctx, result);
        result.setStatus("COMPLETED");
        return result;
    }

    // =========================================================================
    // XML FORMAT PARSER (Secure StAX)
    // =========================================================================

    private ParserResult parseXml(String rawConfig, ParserResult result) {
        ParseContext ctx = new ParseContext();
        String[] lines = rawConfig.split("\\r?\\n");

        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try {
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception ignored) {
            // Some StAX implementations might not support these specific properties
        }

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(rawConfig));
            Deque<String> tagStack = new ArrayDeque<>();
            Deque<String> entryStack = new ArrayDeque<>();

            while (reader.hasNext()) {
                int event = reader.next();
                int lineNum = reader.getLocation() != null ? reader.getLocation().getLineNumber() : 1;
                String rawLine = (lineNum >= 1 && lineNum <= lines.length) ? lines[lineNum - 1] : "";

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String tagName = reader.getLocalName();
                    tagStack.addLast(tagName);

                    String entryName = null;
                    if ("entry".equalsIgnoreCase(tagName)) {
                        entryName = reader.getAttributeValue(null, "name");
                        entryStack.addLast(entryName != null ? entryName : "");
                    }

                    String currentPath = String.join("/", tagStack);

                    // 1. Interface management profile services
                    // Path: .../network/profiles/interface-management-profile/entry/<service>
                    if (currentPath.contains("network/profiles/interface-management-profile/entry")
                            && isProfileServiceTag(tagName)) {
                        String currentProfile = entryStack.isEmpty() ? null : entryStack.peekLast();
                        String text = reader.getElementText();
                        tagStack.removeLast(); // getElementText consumes the END_ELEMENT
                        boolean enabled = "yes".equalsIgnoreCase(text.trim());
                        if (currentProfile != null && !currentProfile.isEmpty()) {
                            ctx.profileServices.computeIfAbsent(currentProfile, k -> new LinkedHashMap<>())
                                    .put(tagName.toLowerCase(), enabled);
                            String field = mapServiceToCanonicalField(tagName.toLowerCase());
                            if (field != null) {
                                ctx.profileServiceSources.computeIfAbsent(currentProfile, k -> new LinkedHashMap<>())
                                        .put(tagName.toLowerCase(), new SourceMapEntry(field, lineNum, rawLine));
                            }
                        }
                        continue;
                    }

                    // 2. Interface attachment: .../network/interface/.../interface-management-profile
                    if (currentPath.contains("network/interface") && "interface-management-profile".equalsIgnoreCase(tagName)) {
                        String profileName = reader.getElementText();
                        tagStack.removeLast();
                        if (profileName != null && !profileName.trim().isEmpty()) {
                            ctx.attachedProfiles.add(profileName.trim());
                            ctx.attachedProfileSources.put(profileName.trim(), new SourceMapEntry("interface-management-profile", lineNum, rawLine));
                        }
                        continue;
                    }

                    // 3. Management interface service settings
                    // Path: .../deviceconfig/system/service/<disable-telnet|disable-ssh|disable-https>
                    if (currentPath.contains("deviceconfig/system/service")) {
                        if ("disable-telnet".equalsIgnoreCase(tagName)) {
                            String text = reader.getElementText();
                            tagStack.removeLast();
                            ctx.mgmtDisableTelnet = "yes".equalsIgnoreCase(text.trim());
                            ctx.mgmtDisableTelnetSource = new SourceMapEntry("security.telnet.enabled", lineNum, rawLine);
                            continue;
                        } else if ("disable-ssh".equalsIgnoreCase(tagName)) {
                            String text = reader.getElementText();
                            tagStack.removeLast();
                            ctx.mgmtDisableSsh = "yes".equalsIgnoreCase(text.trim());
                            ctx.mgmtDisableSshSource = new SourceMapEntry("security.ssh.enabled", lineNum, rawLine);
                            continue;
                        } else if ("disable-https".equalsIgnoreCase(tagName)) {
                            String text = reader.getElementText();
                            tagStack.removeLast();
                            ctx.mgmtDisableHttps = "yes".equalsIgnoreCase(text.trim());
                            ctx.mgmtDisableHttpsSource = new SourceMapEntry("security.https.enabled", lineNum, rawLine);
                            continue;
                        }
                    }

                    // 4. Explicit SSH version: .../deviceconfig/system/ssh-service/version
                    if (currentPath.contains("deviceconfig/system/ssh-service") && "version".equalsIgnoreCase(tagName)) {
                        String text = reader.getElementText();
                        tagStack.removeLast();
                        try {
                            ctx.explicitSshVersion = Integer.parseInt(text.trim());
                            ctx.explicitSshVersionSource = new SourceMapEntry("security.ssh.version", lineNum, rawLine);
                        } catch (NumberFormatException ignored) {}
                        continue;
                    }

                    // 5. SNMP v2c: .../snmp-setting/access-setting/version/v2c/snmp-community-string
                    if (currentPath.contains("snmp-setting/access-setting/version/v2c") && "snmp-community-string".equalsIgnoreCase(tagName)) {
                        String text = reader.getElementText();
                        tagStack.removeLast();
                        if (text != null && !text.trim().isEmpty()) {
                            ctx.snmpV2cFound = true;
                            ctx.snmpV2cSource = new SourceMapEntry("security.snmp.enabled", lineNum, rawLine);
                        }
                        continue;
                    }

                    // 6. SNMP v3: .../snmp-setting/access-setting/version/v3/users/entry
                    if (currentPath.contains("snmp-setting/access-setting/version/v3/users/entry")) {
                        String userName = entryStack.isEmpty() ? null : entryStack.peekLast();
                        if (userName != null && !userName.isEmpty()) {
                            ctx.snmpV3Users.add(userName);
                            ctx.snmpV3Source = new SourceMapEntry("security.snmp.enabled", lineNum, rawLine);
                        }
                    }

                    // 7. Syslog server profile definition
                    // Path: .../log-settings/syslog/entry/.../server/entry/<server>
                    if (currentPath.contains("log-settings/syslog/entry") && "server".equalsIgnoreCase(tagName)
                            && currentPath.endsWith("/server/entry/server")) {
                        String host = reader.getElementText();
                        tagStack.removeLast();
                        // Find profile entry name from stack
                        String profileName = findEnclosingSyslogProfile(entryStack);
                        if (profileName != null && host != null && !host.trim().isEmpty()) {
                            ctx.syslogProfileServers.computeIfAbsent(profileName, k -> new LinkedHashSet<>()).add(host.trim());
                            ctx.syslogProfileSources.put(profileName, new SourceMapEntry("logging.syslog", lineNum, rawLine));
                        }
                        continue;
                    }

                    // 8. Syslog reference: .../send-syslog/member
                    if (currentPath.contains("log-settings") && currentPath.endsWith("send-syslog/member")) {
                        String profileName = reader.getElementText();
                        tagStack.removeLast();
                        if (profileName != null && !profileName.trim().isEmpty()) {
                            ctx.referencedSyslogProfiles.add(profileName.trim());
                            ctx.syslogRefSources.put(profileName.trim(), new SourceMapEntry("logging.syslog", lineNum, rawLine));
                        }
                        continue;
                    }

                    // 9. NTP: .../deviceconfig/system/ntp-servers/.../ntp-server-address
                    if (currentPath.contains("deviceconfig/system/ntp-servers") && "ntp-server-address".equalsIgnoreCase(tagName)) {
                        String address = reader.getElementText();
                        tagStack.removeLast();
                        if (address != null && !address.trim().isEmpty()) {
                            ctx.ntpConfigured = true;
                            ctx.ntpSource = new SourceMapEntry("ntp.configured", lineNum, rawLine);
                        }
                        continue;
                    }

                    // 10. AAA external profile definition: .../authentication-profile/entry/method/(radius|tacplus|ldap|kerberos|saml)
                    if (currentPath.contains("authentication-profile/entry/method") && isExternalAuthMethod(tagName)) {
                        String profileName = entryStack.isEmpty() ? null : entryStack.peekLast();
                        if (profileName != null && !profileName.isEmpty()) {
                            ctx.externalAuthProfiles.add(profileName);
                        }
                    }

                    // 11. AAA admin reference:
                    // .../deviceconfig/system/authentication-profile
                    // or .../mgt-config/users/entry/authentication-profile
                    if ((currentPath.endsWith("deviceconfig/system/authentication-profile")
                            || currentPath.endsWith("mgt-config/users/entry/authentication-profile"))
                            && "authentication-profile".equalsIgnoreCase(tagName)) {
                        String profileName = reader.getElementText();
                        tagStack.removeLast();
                        if (profileName != null && !profileName.trim().isEmpty()) {
                            ctx.adminAuthProfilesUsed.add(profileName.trim());
                            ctx.aaaRefSources.put(profileName.trim(), new SourceMapEntry("authentication.aaa", lineNum, rawLine));
                        }
                        continue;
                    }

                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String tagName = reader.getLocalName();
                    if ("entry".equalsIgnoreCase(tagName) && !entryStack.isEmpty()) {
                        entryStack.removeLast();
                    }
                    if (!tagStack.isEmpty()) {
                        tagStack.removeLast();
                    }
                }
            }

            populateCanonicalFromContext(ctx, result);
            result.setStatus("COMPLETED");
            return result;

        } catch (XMLStreamException ex) {
            int errLine = ex.getLocation() != null ? ex.getLocation().getLineNumber() : 1;
            String raw = (errLine >= 1 && errLine <= lines.length) ? lines[errLine - 1] : "";
            result.setStatus("FAILED");
            result.getUnknowns().add(new UnknownConstruct(raw, errLine, "XML parsing error: " + ex.getMessage()));
            return result;
        } catch (Exception ex) {
            result.setStatus("FAILED");
            result.getUnknowns().add(new UnknownConstruct("", 1, "Parsing error: " + ex.getMessage()));
            return result;
        }
    }

    private String findEnclosingSyslogProfile(Deque<String> entryStack) {
        if (entryStack.isEmpty()) return null;
        // The first entry is the syslog profile name, subsequent entries might be server entries
        Iterator<String> it = entryStack.iterator();
        return it.hasNext() ? it.next() : null;
    }

    private boolean isProfileServiceTag(String tag) {
        String t = tag.toLowerCase();
        return t.equals("ssh") || t.equals("telnet") || t.equals("https")
                || t.equals("http") || t.equals("snmp") || t.equals("ping");
    }

    private boolean isExternalAuthMethod(String tag) {
        String t = tag.toLowerCase();
        return t.equals("radius") || t.equals("tacplus") || t.equals("ldap")
                || t.equals("kerberos") || t.equals("saml");
    }

    private String mapServiceToCanonicalField(String svc) {
        return switch (svc) {
            case "ssh" -> "security.ssh.enabled";
            case "telnet" -> "security.telnet.enabled";
            case "https" -> "security.https.enabled";
            default -> null;
        };
    }

    // =========================================================================
    // CANONICAL RESOLUTION PHASE (Two-Pass, Unified)
    // =========================================================================

    private void populateCanonicalFromContext(ParseContext ctx, ParserResult result) {
        CanonicalSecurityModel model = result.getCanonical();
        List<SourceMapEntry> sourceMap = result.getSourceMap();

        // --- 1. SSH / Telnet / HTTPS Resolution ---
        // Pass 2: Evaluate services on ATTACHED profiles
        boolean attachedHasSsh = false;
        SourceMapEntry attachedSshSource = null;

        boolean attachedHasTelnet = false;
        SourceMapEntry attachedTelnetSource = null;

        boolean attachedHasHttps = false;
        SourceMapEntry attachedHttpsSource = null;

        for (String attachedName : ctx.attachedProfiles) {
            Map<String, Boolean> svcs = ctx.profileServices.get(attachedName);
            if (svcs == null) continue;

            Map<String, SourceMapEntry> sources = ctx.profileServiceSources.get(attachedName);

            if (Boolean.TRUE.equals(svcs.get("ssh"))) {
                attachedHasSsh = true;
                if (sources != null && sources.containsKey("ssh")) {
                    attachedSshSource = sources.get("ssh");
                }
            }
            if (Boolean.TRUE.equals(svcs.get("telnet"))) {
                attachedHasTelnet = true;
                if (sources != null && sources.containsKey("telnet")) {
                    attachedTelnetSource = sources.get("telnet");
                }
            }
            if (Boolean.TRUE.equals(svcs.get("https"))) {
                attachedHasHttps = true;
                if (sources != null && sources.containsKey("https")) {
                    attachedHttpsSource = sources.get("https");
                }
            }
        }

        // SSH: Enabled if attached profile enables it, or mgmt interface enables it (disable-ssh is false)
        if (attachedHasSsh || Boolean.FALSE.equals(ctx.mgmtDisableSsh)) {
            model.setSsh(true, ctx.explicitSshVersion);
            SourceMapEntry src = attachedHasSsh ? attachedSshSource : ctx.mgmtDisableSshSource;
            if (src != null) {
                sourceMap.add(src);
            }
            if (ctx.explicitSshVersion != null && ctx.explicitSshVersionSource != null) {
                sourceMap.add(ctx.explicitSshVersionSource);
            }
        } else if (Boolean.TRUE.equals(ctx.mgmtDisableSsh)) {
            model.setSsh(false, null);
            if (ctx.mgmtDisableSshSource != null) {
                sourceMap.add(ctx.mgmtDisableSshSource);
            }
        }

        // Telnet:
        // Rule: telnet=false only on an affirmative disable statement with no enabling anywhere; otherwise null.
        if (attachedHasTelnet || Boolean.FALSE.equals(ctx.mgmtDisableTelnet)) {
            model.setTelnet(true);
            SourceMapEntry src = attachedHasTelnet ? attachedTelnetSource : ctx.mgmtDisableTelnetSource;
            if (src != null) {
                sourceMap.add(src);
            }
        } else if (Boolean.TRUE.equals(ctx.mgmtDisableTelnet)) {
            // Affirmative disable statement with no enabling anywhere -> false
            model.setTelnet(false);
            if (ctx.mgmtDisableTelnetSource != null) {
                sourceMap.add(ctx.mgmtDisableTelnetSource);
            }
        }
        // If neither enabled nor affirmatively disabled -> remains null/unset

        // HTTPS: Enabled if attached profile enables it, or mgmt interface enables it (disable-https is false)
        if (attachedHasHttps || Boolean.FALSE.equals(ctx.mgmtDisableHttps)) {
            model.setHttps(true);
            SourceMapEntry src = attachedHasHttps ? attachedHttpsSource : ctx.mgmtDisableHttpsSource;
            if (src != null) {
                sourceMap.add(src);
            }
        } else if (Boolean.TRUE.equals(ctx.mgmtDisableHttps)) {
            model.setHttps(false);
            if (ctx.mgmtDisableHttpsSource != null) {
                sourceMap.add(ctx.mgmtDisableHttpsSource);
            }
        }

        // --- 2. SNMP Resolution ---
        // Convention: community sets enabled=true and version=null; v3 user sets enabled=true and version="3"
        // Coexistence: community present leaves version unset (null)
        if (ctx.snmpV2cFound) {
            model.setSnmp(true, null);
            if (ctx.snmpV2cSource != null) {
                sourceMap.add(ctx.snmpV2cSource);
            }
        } else if (!ctx.snmpV3Users.isEmpty()) {
            model.setSnmp(true, "3");
            if (ctx.snmpV3Source != null) {
                sourceMap.add(ctx.snmpV3Source);
                sourceMap.add(new SourceMapEntry("security.snmp.version", ctx.snmpV3Source.getSourceLine(), ctx.snmpV3Source.getRawText()));
            }
        }

        // --- 3. Syslog Resolution ---
        // Syslog server profile with >= 1 server AND referenced from log settings
        boolean syslogActive = false;
        SourceMapEntry syslogSource = null;

        for (String refProfile : ctx.referencedSyslogProfiles) {
            Set<String> srvs = ctx.syslogProfileServers.get(refProfile);
            if (srvs != null && !srvs.isEmpty()) {
                syslogActive = true;
                syslogSource = ctx.syslogRefSources.get(refProfile);
                if (syslogSource == null) {
                    syslogSource = ctx.syslogProfileSources.get(refProfile);
                }
                break;
            }
        }

        if (syslogActive) {
            model.setSyslog(true);
            if (syslogSource != null) {
                sourceMap.add(syslogSource);
            }
        }

        // --- 4. Local Logging ---
        // Deliberately left UNSET per platform rule (do not infer from platform behavior)

        // --- 5. NTP Resolution ---
        if (ctx.ntpConfigured) {
            model.setNtpConfigured(true);
            if (ctx.ntpSource != null) {
                sourceMap.add(ctx.ntpSource);
            }
        }

        // --- 6. AAA Resolution ---
        // External auth profile (radius/tacplus/ldap/kerberos/saml) actually used for admin auth -> true
        boolean aaaUsed = false;
        SourceMapEntry aaaSource = null;

        for (String usedProfile : ctx.adminAuthProfilesUsed) {
            if (ctx.externalAuthProfiles.contains(usedProfile)) {
                aaaUsed = true;
                aaaSource = ctx.aaaRefSources.get(usedProfile);
                break;
            }
        }

        if (aaaUsed) {
            model.setAaa(true);
            if (aaaSource != null) {
                sourceMap.add(aaaSource);
            }
        }
    }
}
