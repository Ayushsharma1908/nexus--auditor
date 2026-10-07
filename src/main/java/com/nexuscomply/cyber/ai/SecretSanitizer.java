package com.nexuscomply.cyber.ai;

import java.util.regex.Pattern;

/**
 * Sanitizes sensitive credentials (passwords, community strings, pre-shared keys, hashes)
 * from configuration syntax lines before storage in ai_mappings, transmission to AI providers,
 * or rendering in reports.
 */
public final class SecretSanitizer {

    public static final String REDACTED = "<REDACTED>";

    // SNMP community strings: snmp-server community <STRING> [RO|RW] ...
    private static final Pattern SNMP_COMMUNITY_PATTERN = Pattern.compile(
            "(?i)\\b(snmp(?:-server)?\\s+community\\s+)\\S+(.*)"
    );

    // User secrets and passwords: username <user> [privilege N] secret|password [type] <secret>
    private static final Pattern USERNAME_SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(username\\s+\\S+(?:\\s+privilege\\s+\\d+)?\\s+(?:secret|password)\\s+)(?:\\d+\\s+)?\\S+.*"
    );

    // Enable secrets and passwords: enable secret|password [type] <secret>
    private static final Pattern ENABLE_SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(enable\\s+(?:secret|password)\\s+)(?:\\d+\\s+)?\\S+.*"
    );

    // Pre-shared keys: pre-shared-key [type] <key>, psk [type] <key>
    private static final Pattern PSK_PATTERN = Pattern.compile(
            "(?i)\\b((?:pre-shared-key|preshared-key|psk)\\s+)(?:(?:local|remote|ascii|hex|\\d+)\\s+)?\\S+.*"
    );

    // Generic password or secret: password [type] <secret> / secret [type] <secret>
    private static final Pattern GENERIC_PASSWORD_PATTERN = Pattern.compile(
            "(?i)\\b((?:password|secret)\\s+)(?:\\d+\\s+)?\\S+.*"
    );

    private SecretSanitizer() {}

    /**
     * Sanitizes credentials from raw configuration line or multi-line text block.
     *
     * @param input raw syntax line or multi-line block
     * @return sanitized string with credentials replaced by <REDACTED>
     */
    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        if (input.contains("\n")) {
            String[] lines = input.split("\r?\n", -1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                sb.append(sanitizeLine(lines[i]));
            }
            return sb.toString();
        }

        return sanitizeLine(input);
    }

    private static String sanitizeLine(String line) {
        if (line == null || line.isBlank()) {
            return line;
        }

        String result = line;

        // 1. SNMP community: snmp-server community SECRET123 RO -> snmp-server community <REDACTED> RO
        if (SNMP_COMMUNITY_PATTERN.matcher(result).find()) {
            result = SNMP_COMMUNITY_PATTERN.matcher(result).replaceAll("$1" + REDACTED + "$2");
        }

        // 2. User secret / password: username admin secret 5 $1$abc -> username admin secret <REDACTED>
        if (USERNAME_SECRET_PATTERN.matcher(result).find()) {
            result = USERNAME_SECRET_PATTERN.matcher(result).replaceAll("$1" + REDACTED);
        }
        // 3. Enable secret / password
        else if (ENABLE_SECRET_PATTERN.matcher(result).find()) {
            result = ENABLE_SECRET_PATTERN.matcher(result).replaceAll("$1" + REDACTED);
        }
        // 4. Pre-shared keys
        else if (PSK_PATTERN.matcher(result).find()) {
            result = PSK_PATTERN.matcher(result).replaceAll("$1" + REDACTED);
        }
        // 5. Generic password / secret
        else if (GENERIC_PASSWORD_PATTERN.matcher(result).find()) {
            result = GENERIC_PASSWORD_PATTERN.matcher(result).replaceAll("$1" + REDACTED);
        }

        return result;
    }
}
