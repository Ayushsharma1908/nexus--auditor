package com.nexuscomply.cyber.parser.cisco;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.UnknownConstruct;
import com.nexuscomply.cyber.parser.VendorParser;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CiscoIosParser implements VendorParser {

    private static final Pattern SSH_VERSION_PATTERN = Pattern.compile("(?i)^\\s*ip\\s+ssh\\s+version\\s+(1|2)\\s*$");
    private static final Pattern NO_IP_SSH_PATTERN = Pattern.compile("(?i)^\\s*no\\s+ip\\s+ssh(\\s+.*)?$");
    private static final Pattern TRANSPORT_INPUT_PATTERN = Pattern.compile("(?i)^\\s*transport\\s+input\\s+(.+)$");
    private static final Pattern HTTPS_ENABLE_PATTERN = Pattern.compile("(?i)^\\s*ip\\s+http\\s+secure-server\\s*$");
    private static final Pattern NO_HTTP_SERVER_PATTERN = Pattern.compile("(?i)^\\s*no\\s+ip\\s+http\\s+server\\s*$");
    private static final Pattern SNMP_V3_PATTERN = Pattern.compile("(?i)^\\s*snmp-server\\b.*\\b(version\\s+3|v3)\\b.*$");
    private static final Pattern AAA_NEW_MODEL_PATTERN = Pattern.compile("(?i)^\\s*aaa\\s+new-model\\s*$");
    private static final Pattern LOGGING_SYSLOG_PATTERN = Pattern.compile("(?i)^\\s*logging\\s+(host\\b|on\\b).*$");
    private static final Pattern LOGGING_LOCAL_PATTERN = Pattern.compile("(?i)^\\s*logging\\s+(buffered\\b|console\\b).*$");
    private static final Pattern NTP_SERVER_PATTERN = Pattern.compile("(?i)^\\s*ntp\\s+server\\b.*$");

    @Override
    public boolean supports(String vendor, String platform) {
        if (vendor != null && "cisco".equalsIgnoreCase(vendor.trim())) {
            return true;
        }
        if (platform != null) {
            String p = platform.trim().toUpperCase();
            if (p.contains("FORTI") || p.contains("JUN") || p.contains("PAN")) {
                return false;
            }
            return p.equals("IOS") || p.equals("IOS-XE") || p.startsWith("IOS-") || p.startsWith("CISCO");
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

            // Ignore blank lines and Cisco comment lines
            if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("#")) {
                continue;
            }

            boolean matched = false;

            // Rule 1: ip ssh version [1|2]
            Matcher sshMatcher = SSH_VERSION_PATTERN.matcher(trimmed);
            if (sshMatcher.matches()) {
                int version = Integer.parseInt(sshMatcher.group(1));
                model.setSsh(true, version);
                result.getSourceMap().add(new SourceMapEntry("security.ssh.version", lineNum, rawLine));
                result.getSourceMap().add(new SourceMapEntry("security.ssh.enabled", lineNum, rawLine));
                matched = true;
            }

            // Rule 2: no ip ssh
            if (!matched && NO_IP_SSH_PATTERN.matcher(trimmed).matches()) {
                model.setSsh(false, null);
                result.getSourceMap().add(new SourceMapEntry("security.ssh.enabled", lineNum, rawLine));
                matched = true;
            }

            // Rule 3: transport input [telnet|ssh|none|all]
            if (!matched) {
                Matcher transportMatcher = TRANSPORT_INPUT_PATTERN.matcher(trimmed);
                if (transportMatcher.matches()) {
                    String transportArgs = transportMatcher.group(1).toLowerCase();
                    if (transportArgs.contains("telnet")) {
                        model.setTelnet(true);
                        result.getSourceMap().add(new SourceMapEntry("security.telnet.enabled", lineNum, rawLine));
                    } else if (transportArgs.contains("ssh") || transportArgs.contains("none")) {
                        model.setTelnet(false);
                        result.getSourceMap().add(new SourceMapEntry("security.telnet.enabled", lineNum, rawLine));
                    }
                    matched = true;
                }
            }

            // Rule 4: ip http secure-server
            if (!matched && HTTPS_ENABLE_PATTERN.matcher(trimmed).matches()) {
                model.setHttps(true);
                result.getSourceMap().add(new SourceMapEntry("security.https.enabled", lineNum, rawLine));
                matched = true;
            }

            // Rule 5: no ip http server (leave https untouched unless explicitly set)
            if (!matched && NO_HTTP_SERVER_PATTERN.matcher(trimmed).matches()) {
                // As per specification: leave https untouched unless explicitly set
                matched = true;
            }

            // Rule 6: snmp-server ... version 3
            if (!matched && SNMP_V3_PATTERN.matcher(trimmed).matches()) {
                model.setSnmp(true, "3");
                result.getSourceMap().add(new SourceMapEntry("security.snmp.enabled", lineNum, rawLine));
                result.getSourceMap().add(new SourceMapEntry("security.snmp.version", lineNum, rawLine));
                matched = true;
            }

            // Rule 7: aaa new-model
            if (!matched && AAA_NEW_MODEL_PATTERN.matcher(trimmed).matches()) {
                model.setAaa(true);
                result.getSourceMap().add(new SourceMapEntry("authentication.aaa", lineNum, rawLine));
                matched = true;
            }

            // Rule 8: logging host / logging on
            if (!matched && LOGGING_SYSLOG_PATTERN.matcher(trimmed).matches()) {
                model.setSyslog(true);
                result.getSourceMap().add(new SourceMapEntry("logging.syslog", lineNum, rawLine));
                matched = true;
            }

            // Rule 9: logging buffered / logging console
            if (!matched && LOGGING_LOCAL_PATTERN.matcher(trimmed).matches()) {
                model.setLocalLogging(true);
                result.getSourceMap().add(new SourceMapEntry("logging.localLogging", lineNum, rawLine));
                matched = true;
            }

            // Rule 10: ntp server
            if (!matched && NTP_SERVER_PATTERN.matcher(trimmed).matches()) {
                model.setNtpConfigured(true);
                result.getSourceMap().add(new SourceMapEntry("ntp.configured", lineNum, rawLine));
                matched = true;
            }

            // If not matched by any extraction rule, record in unknowns
            if (!matched) {
                result.getUnknowns().add(new UnknownConstruct(
                        rawLine,
                        lineNum,
                        "Unrecognized or unsupported Cisco CLI command"
                ));
            }
        }

        result.setStatus("COMPLETED");
        return result;
    }
}
