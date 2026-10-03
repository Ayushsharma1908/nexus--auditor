package com.nexuscomply.cyber.detection;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class VendorFingerprintDetectionService implements VendorDetectionService {

    private static final Map<String, Pattern[]> VENDOR_CLUES = new HashMap<>();

    static {
        VENDOR_CLUES.put("Cisco", new Pattern[]{
                Pattern.compile("(?i)^\\s*version\\s+\\d+(\\.\\d+)*"),
                Pattern.compile("(?i)^\\s*hostname\\s+\\S+"),
                Pattern.compile("(?i)^\\s*(enable\\s+secret|service\\s+password-encryption)"),
                Pattern.compile("(?i)^\\s*line\\s+(vty|con|aux)\\b"),
                Pattern.compile("(?i)^\\s*(ip\\s+ssh|ip\\s+domain-name)\\b"),
                Pattern.compile("(?i)^\\s*interface\\s+(GigabitEthernet|TenGigabitEthernet|FastEthernet|Ethernet|Vlan|Loopback)\\s*\\d"),
                Pattern.compile("(?i)^\\s*aaa\\s+new-model\\b")
        });

        VENDOR_CLUES.put("Juniper", new Pattern[]{
                Pattern.compile("(?i)^\\s*set\\s+system\\b"),
                Pattern.compile("(?i)^\\s*set\\s+interfaces\\b"),
                Pattern.compile("(?i)^\\s*set\\s+protocols\\b"),
                Pattern.compile("(?i)^\\s*set\\s+security\\b"),
                Pattern.compile("(?i)^\\s*apply-groups\\b"),
                Pattern.compile("(?i)junos")
        });

        VENDOR_CLUES.put("Fortinet", new Pattern[]{
                Pattern.compile("(?i)^\\s*config\\s+system\\b"),
                Pattern.compile("(?i)^\\s*config\\s+firewall\\b"),
                Pattern.compile("(?i)^\\s*config\\s+router\\b"),
                Pattern.compile("(?i)^\\s*set\\s+vdom\\b"),
                Pattern.compile("(?i)^\\s*end\\s*$"),
                Pattern.compile("(?i)fortigate")
        });

        VENDOR_CLUES.put("Palo Alto", new Pattern[]{
                Pattern.compile("(?i)^\\s*set\\s+deviceconfig\\b"),
                Pattern.compile("(?i)^\\s*set\\s+network\\b"),
                Pattern.compile("(?i)^\\s*set\\s+zone\\b"),
                Pattern.compile("(?i)^\\s*set\\s+shared\\b"),
                Pattern.compile("(?i)pan-os"),
                Pattern.compile("(?i)<entry\\s+name=\"localhost\\.localdomain\">"),
                Pattern.compile("(?i)<deviceconfig\\b"),
                Pattern.compile("(?i)paloaltonetworks"),
                Pattern.compile("(?i)<interface-management-profile\\b")
        });
    }

    private static final Pattern CISCO_IOS_XE_VERSION_PATTERN = Pattern.compile("(?m)^\\s*version\\s+(?:16|17|18|3)\\.");

    @Override
    public VendorDetectionResponse detectVendor(String rawConfig) {
        return detectVendor(new VendorDetectionRequest(rawConfig));
    }

    @Override
    public VendorDetectionResponse detectVendor(VendorDetectionRequest request) {
        if (request == null || request.getRawConfig() == null || request.getRawConfig().trim().isEmpty()) {
            return new VendorDetectionResponse("UNKNOWN", "UNKNOWN", 0.0, "SYNTAX_FINGERPRINT", VendorDetectionStatus.UNKNOWN);
        }

        String rawConfig = request.getRawConfig();
        boolean isXml = rawConfig.trim().startsWith("<") || rawConfig.contains("<?xml");

        Map<String, Integer> matchCounts = new HashMap<>();
        for (String vendor : VENDOR_CLUES.keySet()) {
            matchCounts.put(vendor, 0);
        }

        if (isXml) {
            // For XML configurations (including minified single-line exports), count distinct clue patterns matched across the text
            for (Map.Entry<String, Pattern[]> entry : VENDOR_CLUES.entrySet()) {
                String vendor = entry.getKey();
                int distinctMatches = 0;
                for (Pattern p : entry.getValue()) {
                    if (p.matcher(rawConfig).find()) {
                        distinctMatches++;
                    }
                }
                matchCounts.put(vendor, distinctMatches);
            }
        } else {
            String[] lines = rawConfig.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                for (Map.Entry<String, Pattern[]> entry : VENDOR_CLUES.entrySet()) {
                    String vendor = entry.getKey();
                    for (Pattern p : entry.getValue()) {
                        if (p.matcher(trimmed).find()) {
                            matchCounts.put(vendor, matchCounts.get(vendor) + 1);
                            break; // count at most one pattern match per line per vendor
                        }
                    }
                }
            }
        }

        String bestVendor = "UNKNOWN";
        int maxMatches = 0;
        boolean tie = false;
        for (Map.Entry<String, Integer> entry : matchCounts.entrySet()) {
            if (entry.getValue() > maxMatches) {
                maxMatches = entry.getValue();
                bestVendor = entry.getKey();
                tie = false;
            } else if (entry.getValue() == maxMatches && maxMatches > 0) {
                tie = true;
            }
        }

        if (maxMatches == 0) {
            return new VendorDetectionResponse("UNKNOWN", "UNKNOWN", 0.0, "SYNTAX_FINGERPRINT", VendorDetectionStatus.UNKNOWN);
        }

        double confidence = Math.min(0.99, 0.50 + (maxMatches * 0.10));

        // A tie between multiple vendors must produce UNCERTAIN, not arbitrarily picking the first map entry
        if (tie) {
            return new VendorDetectionResponse("UNKNOWN", "UNKNOWN", confidence, "SYNTAX_FINGERPRINT", VendorDetectionStatus.UNCERTAIN);
        }

        String platform = "UNKNOWN";

        if ("Cisco".equalsIgnoreCase(bestVendor)) {
            platform = rawConfig.contains("IOS-XE") || CISCO_IOS_XE_VERSION_PATTERN.matcher(rawConfig).find() ? "IOS-XE" : "IOS";
        } else if ("Juniper".equalsIgnoreCase(bestVendor)) {
            platform = "JUNOS";
        } else if ("Fortinet".equalsIgnoreCase(bestVendor)) {
            platform = "FortiOS";
        } else if ("Palo Alto".equalsIgnoreCase(bestVendor)) {
            platform = "PAN-OS";
        }

        VendorDetectionStatus status = confidence >= 0.70 ? VendorDetectionStatus.DETECTED : VendorDetectionStatus.UNCERTAIN;
        return new VendorDetectionResponse(bestVendor, platform, confidence, "SYNTAX_FINGERPRINT", status);
    }
}
