package com.nexuscomply.cyber.parser.fortinet;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.UnknownConstruct;
import com.nexuscomply.cyber.parser.VendorParser;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Fortinet FortiOS configuration files in block-structured format (config ... set ... end).
 *
 * <p>Architectural Notes & Vendor-Specific Interpretations:
 * <ul>
 *   <li><b>Block-Structured Context:</b> FortiOS uses hierarchical blocks delimited by {@code config <section>},
 *       {@code edit <entry>}, {@code next}, and {@code end}. A stack tracks active blocks so that identical
 *       {@code set} commands (e.g., {@code set status enable}) are interpreted strictly within their enclosing context.</li>
 *   <li><b>Admin Access (SSH, Telnet, HTTPS):</b> Governed by {@code set allowaccess} under {@code config system interface}.
 *       Presence of {@code ssh} sets {@code security.ssh.enabled = true}.
 *       Presence of {@code telnet} sets {@code security.telnet.enabled = true}.
 *       Presence of {@code https} sets {@code security.https.enabled = true}.</li>
 *   <li><b>SSH Version:</b> FortiOS does not expose an explicit CLI toggle for SSH protocol version (e.g., v1 vs v2).
 *       Therefore, {@code security.ssh.version} is intentionally left UNSET (null). This is a platform capability gap,
 *       not a parser omission.</li>
 *   <li><b>Telnet Asymmetry:</b> Absence of {@code telnet} in interface {@code allowaccess} lists leaves
 *       {@code security.telnet.enabled} UNSET (null), not {@code false}, because FortiOS configuration alone does not
 *       prove affirmative administrative disablement (same principle as Junos).</li>
 *   <li><b>SNMP:</b> {@code config system snmp sysinfo} with {@code set status enable} sets {@code security.snmp.enabled = true}
 *       with version left unset. {@code config system snmp user} with {@code set security-level auth-priv} (or auth-no-priv)
 *       indicates SNMP v3, extracting {@code security.snmp.enabled = true} and {@code security.snmp.version = "3"}.</li>
 *   <li><b>Syslog & Local Logging:</b> {@code config log syslogd setting} with {@code set status enable} (or server configuration)
 *       sets {@code logging.syslog = true}. {@code config log memory setting} (or {@code config log disk setting}) with
 *       {@code set status enable} sets {@code logging.localLogging = true}.</li>
 *   <li><b>NTP:</b> {@code config system ntp} with {@code set ntpsync enable} sets {@code ntp.configured = true}.</li>
 *   <li><b>AAA Equivalence Decision:</b> FortiOS does not have a global {@code aaa new-model} toggle. Local administrator
 *       accounts under {@code config system admin} represent local credentials, not centralized AAA. Conflating local accounts
 *       with AAA would produce misleading compliance results for controls requiring centralized authentication.
 *       Therefore, {@code authentication.aaa} is deliberately left UNSET when only local accounts exist, and is set to true
 *       only if external centralized AAA (such as {@code config user radius} or {@code config user tacacs+}) is observed.</li>
 * </ul>
 */
@Component
public class FortinetFortiOSParser implements VendorParser {

    private static final Pattern CONFIG_PATTERN = Pattern.compile("(?i)^config\\s+(.+)$");
    private static final Pattern EDIT_PATTERN = Pattern.compile("(?i)^edit\\s+(.+)$");
    private static final Pattern ALLOWACCESS_PATTERN = Pattern.compile("(?i)^set\\s+allowaccess\\s+(.+)$");
    private static final Pattern STATUS_ENABLE_PATTERN = Pattern.compile("(?i)^set\\s+status\\s+enable\\s*$");
    private static final Pattern STATUS_DISABLE_PATTERN = Pattern.compile("(?i)^set\\s+status\\s+disable\\s*$");
    private static final Pattern SNMP_V3_SEC_LEVEL_PATTERN = Pattern.compile("(?i)^set\\s+security-level\\s+(auth-priv|auth-no-priv|no-auth-no-priv)\\b.*$");
    private static final Pattern SYSLOG_SERVER_PATTERN = Pattern.compile("(?i)^set\\s+server\\s+\\S+.*$");
    private static final Pattern NTP_SYNC_PATTERN = Pattern.compile("(?i)^set\\s+ntpsync\\s+enable\\s*$");

    private static final Set<String> RECOGNIZED_CONFIG_PREFIXES = Set.of(
            "system interface",
            "system snmp sysinfo",
            "system snmp user",
            "system snmp community",
            "system snmp",
            "log syslogd setting",
            "log memory setting",
            "log disk setting",
            "system ntp",
            "system admin",
            "user radius",
            "user tacacs+"
    );

    @Override
    public boolean supports(String vendor, String platform) {
        if (vendor != null) {
            String v = vendor.trim().toUpperCase();
            if (v.contains("CISCO") || v.contains("JUNIPER") || v.contains("PALO") || v.contains("PAN")) {
                return false;
            }
            if (v.equals("FORTINET") || v.contains("FORTINET") || v.contains("FORTIGATE")) {
                return true;
            }
        }
        if (platform != null) {
            String p = platform.trim().toUpperCase();
            if (p.equals("IOS") || p.startsWith("IOS") || p.contains("CISCO") || p.contains("JUNOS") || p.contains("PAN")) {
                return false;
            }
            return p.equals("FORTIOS") || p.startsWith("FORTI");
        }
        return false;
    }

    @Override
    public ParserResult parse(String rawConfig) {
        ParserResult result = new ParserResult();

        if (rawConfig == null || rawConfig.trim().isEmpty()) {
            result.setStatus("FAILED");
            result.getUnknowns().add(new UnknownConstruct("", 0, "Configuration content is empty or null"));
            return result;
        }

        CanonicalSecurityModel model = result.getCanonical();
        String[] lines = rawConfig.split("\\r?\\n");
        Deque<String> blockStack = new ArrayDeque<>();

        // State tracking for Syslog (requires BOTH status enable AND server set, without disable)
        boolean syslogStatusEnabled = false;
        boolean syslogStatusDisabled = false;
        boolean syslogServerFound = false;
        List<SourceMapEntry> syslogSourceMapEntries = new java.util.ArrayList<>();

        // State tracking for SNMP (distinguish community v2c vs v3 user vs mixed)
        boolean snmpSysinfoEnabled = false;
        boolean snmpCommunityFound = false;
        boolean snmpV3UserFound = false;
        List<SourceMapEntry> snmpEnabledSourceMap = new java.util.ArrayList<>();
        List<SourceMapEntry> snmpVersionSourceMap = new java.util.ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String rawLine = lines[i];
            String trimmed = rawLine.trim();

            // Ignore blank lines and FortiOS comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            // Handle block start: config <section>
            Matcher configMatcher = CONFIG_PATTERN.matcher(trimmed);
            if (configMatcher.matches()) {
                String section = configMatcher.group(1).trim().toLowerCase();
                blockStack.push("config:" + section);
                if (!isRecognizedConfigSection(section)) {
                    result.getUnknowns().add(new UnknownConstruct(
                            rawLine,
                            lineNum,
                            "Unrecognized or unsupported FortiOS config block: " + section
                    ));
                }
                continue;
            }

            // Handle entry start: edit <entry>
            Matcher editMatcher = EDIT_PATTERN.matcher(trimmed);
            if (editMatcher.matches()) {
                String entry = editMatcher.group(1).trim();
                blockStack.push("edit:" + entry);
                continue;
            }

            // Handle entry close: next
            if (trimmed.equalsIgnoreCase("next")) {
                if (!blockStack.isEmpty() && blockStack.peek().startsWith("edit:")) {
                    blockStack.pop();
                }
                continue;
            }

            // Handle block close: end
            if (trimmed.equalsIgnoreCase("end")) {
                while (!blockStack.isEmpty()) {
                    String popped = blockStack.pop();
                    if (popped.startsWith("config:")) {
                        break;
                    }
                }
                continue;
            }

            // At this point, the line is a statement within a block (usually 'set ...' or similar)
            boolean matched = false;

            // Rule 1: Admin access on interfaces (SSH, Telnet, HTTPS)
            if (isInConfig(blockStack, "system interface") && isInEdit(blockStack)) {
                Matcher allowaccessMatcher = ALLOWACCESS_PATTERN.matcher(trimmed);
                if (allowaccessMatcher.matches()) {
                    String[] tokens = allowaccessMatcher.group(1).toLowerCase().split("\\s+");
                    Set<String> protocols = new HashSet<>(Arrays.asList(tokens));

                    if (protocols.contains("ssh")) {
                        model.setSsh(true, null); // version intentionally unset per Rule 8
                        result.getSourceMap().add(new SourceMapEntry("security.ssh.enabled", lineNum, rawLine));
                    }
                    if (protocols.contains("telnet")) {
                        model.setTelnet(true);
                        result.getSourceMap().add(new SourceMapEntry("security.telnet.enabled", lineNum, rawLine));
                    }
                    if (protocols.contains("https")) {
                        model.setHttps(true);
                        result.getSourceMap().add(new SourceMapEntry("security.https.enabled", lineNum, rawLine));
                    }
                    matched = true;
                }
            }

            // Rule 2 & 3: SNMP blocks
            // Community block check (v1/v2c)
            if (!matched && isInConfig(blockStack, "system snmp community", "snmp community")) {
                snmpCommunityFound = true;
                if (trimmed.startsWith("edit ") || trimmed.startsWith("set ")) {
                    snmpEnabledSourceMap.add(new SourceMapEntry("security.snmp.enabled", lineNum, rawLine));
                    matched = true;
                }
            }

            // Sysinfo block check
            if (!matched && isInConfig(blockStack, "system snmp sysinfo", "snmp sysinfo")) {
                if (STATUS_ENABLE_PATTERN.matcher(trimmed).matches()) {
                    snmpSysinfoEnabled = true;
                    snmpEnabledSourceMap.add(new SourceMapEntry("security.snmp.enabled", lineNum, rawLine));
                    matched = true;
                }
            }

            // SNMP v3 user check
            if (!matched && isInConfig(blockStack, "system snmp user", "snmp user") && isInEdit(blockStack)) {
                if (SNMP_V3_SEC_LEVEL_PATTERN.matcher(trimmed).matches()) {
                    snmpV3UserFound = true;
                    snmpEnabledSourceMap.add(new SourceMapEntry("security.snmp.enabled", lineNum, rawLine));
                    snmpVersionSourceMap.add(new SourceMapEntry("security.snmp.version", lineNum, rawLine));
                    matched = true;
                }
            }

            // Rule 4: Remote Syslog (config log syslogd setting) - requires BOTH status enable AND server
            if (!matched && isInConfig(blockStack, "log syslogd setting", "syslogd setting")) {
                if (STATUS_ENABLE_PATTERN.matcher(trimmed).matches()) {
                    syslogStatusEnabled = true;
                    syslogSourceMapEntries.add(new SourceMapEntry("logging.syslog", lineNum, rawLine));
                    matched = true;
                } else if (STATUS_DISABLE_PATTERN.matcher(trimmed).matches()) {
                    syslogStatusDisabled = true;
                    matched = true;
                } else if (SYSLOG_SERVER_PATTERN.matcher(trimmed).matches()) {
                    syslogServerFound = true;
                    syslogSourceMapEntries.add(new SourceMapEntry("logging.syslog", lineNum, rawLine));
                    matched = true;
                }
            }

            // Rule 5: Local Logging (config log memory setting or config log disk setting)
            if (!matched && isInConfig(blockStack, "log memory setting", "log disk setting")) {
                if (STATUS_ENABLE_PATTERN.matcher(trimmed).matches()) {
                    model.setLocalLogging(true);
                    result.getSourceMap().add(new SourceMapEntry("logging.localLogging", lineNum, rawLine));
                    matched = true;
                }
            }

            // Rule 6: NTP (config system ntp)
            if (!matched && isInConfig(blockStack, "system ntp")) {
                if (NTP_SYNC_PATTERN.matcher(trimmed).matches()) {
                    model.setNtpConfigured(true);
                    result.getSourceMap().add(new SourceMapEntry("ntp.configured", lineNum, rawLine));
                    matched = true;
                }
            }

            // Rule 7: External centralized AAA (config user radius or config user tacacs+)
            if (!matched && isInConfig(blockStack, "user radius", "user tacacs+")) {
                model.setAaa(true);
                result.getSourceMap().add(new SourceMapEntry("authentication.aaa", lineNum, rawLine));
                matched = true;
            }

            // If not matched by any extraction rule, record in unknowns
            if (!matched) {
                result.getUnknowns().add(new UnknownConstruct(
                        rawLine,
                        lineNum,
                        "Unrecognized or unsupported FortiOS command"
                ));
            }
        }

        // Finalize Syslog: requires BOTH status enable AND server set, without explicit disable
        if (syslogStatusEnabled && !syslogStatusDisabled && syslogServerFound) {
            model.setSyslog(true);
            result.getSourceMap().addAll(syslogSourceMapEntries);
        }

        // Finalize SNMP:
        // (a) community-only: enabled=true, version=null
        // (b) community + v3 user: enabled=true, version=null (mixed mode, NOT "3")
        // (c) v3 user only: enabled=true, version="3"
        if (snmpSysinfoEnabled || snmpCommunityFound || snmpV3UserFound) {
            model.setSnmp(true, null);
            result.getSourceMap().addAll(snmpEnabledSourceMap);
            if (snmpV3UserFound && !snmpCommunityFound) {
                model.setSnmp(true, "3");
                result.getSourceMap().addAll(snmpVersionSourceMap);
            }
        }

        result.setStatus("COMPLETED");
        return result;
    }

    private boolean isInConfig(Deque<String> stack, String... targets) {
        String fullPath = stack.stream()
                .filter(s -> s.startsWith("config:"))
                .map(s -> s.substring(7).trim().toLowerCase())
                .reduce((sub, parent) -> parent + " " + sub)
                .orElse("");

        for (String target : targets) {
            String t = target.toLowerCase();
            for (String s : stack) {
                if (s.startsWith("config:") && s.substring(7).trim().toLowerCase().contains(t)) {
                    return true;
                }
            }
            if (fullPath.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInEdit(Deque<String> stack) {
        return stack.stream().anyMatch(s -> s.startsWith("edit:"));
    }

    private boolean isRecognizedConfigSection(String section) {
        String secLower = section.toLowerCase();
        return RECOGNIZED_CONFIG_PREFIXES.stream().anyMatch(secLower::contains);
    }
}
