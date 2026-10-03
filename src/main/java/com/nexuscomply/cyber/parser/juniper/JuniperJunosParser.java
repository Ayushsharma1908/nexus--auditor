package com.nexuscomply.cyber.parser.juniper;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.UnknownConstruct;
import com.nexuscomply.cyber.parser.VendorParser;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Juniper Junos configuration files in hierarchical 'set' format.
 * Extracts canonical facts for security, authentication, logging, and NTP.
 *
 * <p>Architectural Notes & Vendor-Specific Interpretations:
 * <ul>
 *   <li><b>SSH Version:</b> Explicit {@code protocol-version v2} extracts version 2. If {@code set system services ssh}
 *       appears without a protocol-version statement, SSH is marked enabled=true but version is left unset,
 *       avoiding unverified version assumptions.</li>
 *   <li><b>Telnet Asymmetry:</b> In Junos, absence of {@code set system services telnet} does not indicate explicit
 *       disablement the way Cisco's {@code transport input ssh} does. Therefore, telnet is left UNSET (null) when the
 *       line is absent, and set to true ONLY when explicitly configured.</li>
 *   <li><b>AAA Equivalence (Absolute Rule 7):</b> Junos has no single {@code aaa new-model} toggle. The presence of a
 *       centralized authentication order in {@code set system authentication-order} (e.g., specifying radius or tacplus
 *       alongside or in place of password) is used as the closest equivalent signal for {@code authentication.aaa = true}.
 *       This is an intentional vendor-specific proxy, not a literal command parallel.</li>
 *   <li><b>SNMP:</b> Lines starting with {@code set snmp v3} extract version="3" and enabled=true. Community-based lines
 *       extract enabled=true with version left unset.</li>
 * </ul>
 */
@Component
public class JuniperJunosParser implements VendorParser {

    private static final Pattern SSH_VERSION_PATTERN = Pattern.compile("(?i)^\\s*set\\s+system\\s+services\\s+ssh\\s+protocol-version\\s+(v1|v2)\\s*$");
    private static final Pattern SSH_ENABLE_PATTERN = Pattern.compile("(?i)^\\s*set\\s+system\\s+services\\s+ssh(\\s+.*)?$");
    private static final Pattern TELNET_PATTERN = Pattern.compile("(?i)^\\s*set\\s+system\\s+services\\s+telnet(\\s+.*)?$");
    private static final Pattern HTTPS_PATTERN = Pattern.compile("(?i)^\\s*set\\s+system\\s+services\\s+web-management\\s+https(\\s+.*)?$");
    private static final Pattern SNMP_V3_PATTERN = Pattern.compile("(?i)^\\s*set\\s+snmp\\s+v3\\b.*$");
    private static final Pattern SNMP_COMMUNITY_PATTERN = Pattern.compile("(?i)^\\s*set\\s+snmp\\s+community\\b.*$");
    private static final Pattern AUTH_ORDER_PATTERN = Pattern.compile("(?i)^\\s*set\\s+system\\s+authentication-order\\s+(.+)$");
    private static final Pattern SYSLOG_HOST_PATTERN = Pattern.compile("(?i)^\\s*set\\s+system\\s+syslog\\s+host\\s+\\S+.*$");
    private static final Pattern SYSLOG_FILE_PATTERN = Pattern.compile("(?i)^\\s*set\\s+system\\s+syslog\\s+file\\s+\\S+.*$");
    private static final Pattern NTP_SERVER_PATTERN = Pattern.compile("(?i)^\\s*set\\s+system\\s+ntp\\s+server\\s+\\S+.*$");

    @Override
    public boolean supports(String vendor, String platform) {
        if (vendor != null) {
            String v = vendor.trim().toUpperCase();
            if (v.contains("CISCO") || v.contains("FORTI") || v.contains("PALO") || v.contains("PAN")) {
                return false;
            }
            if (v.equals("JUNIPER") || v.contains("JUNIPER")) {
                return true;
            }
        }
        if (platform != null) {
            String p = platform.trim().toUpperCase();
            if (p.contains("IOS") || p.contains("FORTI") || p.contains("PAN")) {
                return false;
            }
            return p.equals("JUNOS") || p.startsWith("JUNOS") || p.startsWith("JUNIPER");
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

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String rawLine = lines[i];
            String trimmed = rawLine.trim();

            // Ignore blank lines and Junos comments
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("/*")) {
                continue;
            }

            boolean matched = false;

            // Rule 1: SSH explicit protocol-version
            Matcher sshVerMatcher = SSH_VERSION_PATTERN.matcher(trimmed);
            if (sshVerMatcher.matches()) {
                String verStr = sshVerMatcher.group(1).toLowerCase();
                int version = verStr.contains("2") ? 2 : 1;
                model.setSsh(true, version);
                result.getSourceMap().add(new SourceMapEntry("security.ssh.version", lineNum, rawLine));
                result.getSourceMap().add(new SourceMapEntry("security.ssh.enabled", lineNum, rawLine));
                matched = true;
            }

            // Rule 2: SSH service enabled without explicit version
            if (!matched && SSH_ENABLE_PATTERN.matcher(trimmed).matches()) {
                model.setSsh(true, null);
                result.getSourceMap().add(new SourceMapEntry("security.ssh.enabled", lineNum, rawLine));
                matched = true;
            }

            // Rule 3: Telnet service enabled
            if (!matched && TELNET_PATTERN.matcher(trimmed).matches()) {
                model.setTelnet(true);
                result.getSourceMap().add(new SourceMapEntry("security.telnet.enabled", lineNum, rawLine));
                matched = true;
            }

            // Rule 4: Web-management HTTPS enabled
            if (!matched && HTTPS_PATTERN.matcher(trimmed).matches()) {
                model.setHttps(true);
                result.getSourceMap().add(new SourceMapEntry("security.https.enabled", lineNum, rawLine));
                matched = true;
            }

            // Rule 5: SNMP v3
            if (!matched && SNMP_V3_PATTERN.matcher(trimmed).matches()) {
                model.setSnmp(true, "3");
                result.getSourceMap().add(new SourceMapEntry("security.snmp.enabled", lineNum, rawLine));
                result.getSourceMap().add(new SourceMapEntry("security.snmp.version", lineNum, rawLine));
                matched = true;
            }

            // Rule 6: SNMP community (v1/v2c, version left unset)
            if (!matched && SNMP_COMMUNITY_PATTERN.matcher(trimmed).matches()) {
                model.setSnmp(true, null);
                result.getSourceMap().add(new SourceMapEntry("security.snmp.enabled", lineNum, rawLine));
                matched = true;
            }

            // Rule 7: Authentication order (AAA proxy per Absolute Rule 7)
            if (!matched) {
                Matcher authMatcher = AUTH_ORDER_PATTERN.matcher(trimmed);
                if (authMatcher.matches()) {
                    String authArgs = authMatcher.group(1).replaceAll("[\\[\\]]", "").trim().toLowerCase();
                    boolean hasExternalAuth = Arrays.stream(authArgs.split("\\s+"))
                            .anyMatch(token -> !token.isBlank() && !token.equalsIgnoreCase("password"));
                    if (hasExternalAuth) {
                        model.setAaa(true);
                        result.getSourceMap().add(new SourceMapEntry("authentication.aaa", lineNum, rawLine));
                    }
                    matched = true;
                }
            }

            // Rule 8: Syslog remote host
            if (!matched && SYSLOG_HOST_PATTERN.matcher(trimmed).matches()) {
                model.setSyslog(true);
                result.getSourceMap().add(new SourceMapEntry("logging.syslog", lineNum, rawLine));
                matched = true;
            }

            // Rule 9: Syslog local file
            if (!matched && SYSLOG_FILE_PATTERN.matcher(trimmed).matches()) {
                model.setLocalLogging(true);
                result.getSourceMap().add(new SourceMapEntry("logging.localLogging", lineNum, rawLine));
                matched = true;
            }

            // Rule 10: NTP server
            if (!matched && NTP_SERVER_PATTERN.matcher(trimmed).matches()) {
                model.setNtpConfigured(true);
                result.getSourceMap().add(new SourceMapEntry("ntp.configured", lineNum, rawLine));
                matched = true;
            }

            // Unmatched non-comment lines go into unknowns
            if (!matched) {
                result.getUnknowns().add(new UnknownConstruct(
                        rawLine,
                        lineNum,
                        "Unrecognized or unsupported Juniper Junos command"
                ));
            }
        }

        result.setStatus("COMPLETED");
        return result;
    }
}
