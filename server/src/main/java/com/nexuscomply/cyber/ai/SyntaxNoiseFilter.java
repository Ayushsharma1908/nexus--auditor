package com.nexuscomply.cyber.ai;

import java.util.regex.Pattern;

/**
 * Filters out non-security metadata lines from being captured into the ai_mappings
 * review queue (status PENDING_REVIEW).
 */
public final class SyntaxNoiseFilter {

    private static final Pattern NOISE_PATTERN = Pattern.compile(
            "(?i)^\\s*("
            + "!.*|"                               // Cisco/Fortinet/general comments
            + "#.*|"                               // Unix/Python/general comments
            + ";.*|"                               // Juniper/general comments
            + "/[/*].*|"                           // C-style comments
            + "hostname\\b.*|"                     // Hostname
            + "sysname\\b.*|"                      // Sysname (Huawei/HP)
            + "set\\s+system\\s+host-name\\b.*|"   // Juniper hostname
            + "version\\b.*|"                      // OS version lines (e.g. version 17.6)
            + "set\\s+system\\s+version\\b.*|"     // Juniper version
            + "banner\\b.*|"                       // Banners (motd, exec, login)
            + "description\\b.*|"                  // Interface or entity description
            + ".*\\bdescription\\s+.*|"            // Generic description line
            + "\\^.*|"                             // Banner delimiters
            + "end\\s*|"                           // Block ends
            + "exit\\s*|"                          // Exit commands
            + "quit\\s*"                           // Quit commands
            + ")$"
    );

    private SyntaxNoiseFilter() {}

    /**
     * Determines whether a raw syntax line is non-actionable noise.
     *
     * @param line raw syntax line
     * @return true if the line is non-security metadata or noise, false if potentially actionable
     */
    public static boolean isNoise(String line) {
        if (line == null || line.isBlank()) {
            return true;
        }
        return NOISE_PATTERN.matcher(line.trim()).matches();
    }
}
