package com.nexuscomply.cyber.compliance;

import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultRuleApplicabilityChecker implements RuleApplicabilityChecker {

    @Override
    public boolean isApplicable(ComplianceRule rule, String vendor, String platform, String osVersion) {
        if (rule == null) {
            return false;
        }

        // Vendor applicability check
        List<String> vendors = rule.getApplicableVendors();
        if (vendors != null && !vendors.isEmpty()) {
            if (vendor == null) {
                return false;
            }
            boolean vendorMatches = vendors.stream()
                    .anyMatch(v -> v != null && v.equalsIgnoreCase(vendor.trim()));
            if (!vendorMatches) {
                return false;
            }
        }

        // Platform applicability check
        List<String> platforms = rule.getApplicablePlatforms();
        if (platforms != null && !platforms.isEmpty()) {
            if (platform == null) {
                return false;
            }
            boolean platformMatches = platforms.stream()
                    .anyMatch(p -> p != null && p.equalsIgnoreCase(platform.trim()));
            if (!platformMatches) {
                return false;
            }
        }

        // OS version applicability check
        List<String> osVersions = rule.getApplicableOsVersions();
        if (osVersions != null && !osVersions.isEmpty()) {
            if (osVersion == null) {
                return false;
            }
            boolean osMatches = osVersions.stream()
                    .anyMatch(v -> v != null && (osVersion.trim().equalsIgnoreCase(v.trim()) || osVersion.trim().startsWith(v.trim())));
            if (!osMatches) {
                return false;
            }
        }

        // If no restrictions or all specified restrictions matched, rule is applicable
        return true;
    }
}
