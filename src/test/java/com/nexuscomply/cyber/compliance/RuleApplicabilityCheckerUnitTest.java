package com.nexuscomply.cyber.compliance;

import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleApplicabilityCheckerUnitTest {

    private RuleApplicabilityChecker checker;

    @BeforeEach
    void setUp() {
        checker = new DefaultRuleApplicabilityChecker();
    }

    @Test
    @DisplayName("Matching vendor returns true")
    void testMatchingVendor() {
        ComplianceRule rule = new ComplianceRule();
        rule.setApplicableVendors(List.of("Cisco", "Arista"));

        assertThat(checker.isApplicable(rule, "Cisco", "IOS-XE", "17.6")).isTrue();
        assertThat(checker.isApplicable(rule, "cisco", "IOS", "15.0")).isTrue();
    }

    @Test
    @DisplayName("Non-matching vendor (e.g. Juniper rule evaluated against Cisco) returns false")
    void testNonMatchingVendor() {
        ComplianceRule rule = new ComplianceRule();
        rule.setApplicableVendors(List.of("Juniper"));

        assertThat(checker.isApplicable(rule, "Cisco", "IOS-XE", "17.6")).isFalse();
    }

    @Test
    @DisplayName("Rule with no applicability restriction applies to all vendors and platforms")
    void testNoRestrictionAppliesToAll() {
        ComplianceRule rule = new ComplianceRule();
        rule.setApplicableVendors(List.of());
        rule.setApplicablePlatforms(null);

        assertThat(checker.isApplicable(rule, "Cisco", "IOS-XE", "17.6")).isTrue();
        assertThat(checker.isApplicable(rule, "Juniper", "JUNOS", "21.4")).isTrue();
        assertThat(checker.isApplicable(rule, "Fortinet", "FortiOS", "7.2")).isTrue();
        assertThat(checker.isApplicable(rule, "Palo Alto", "PAN-OS", "10.1")).isTrue();
    }

    @Test
    @DisplayName("Matching platform returns true and non-matching platform returns false")
    void testPlatformApplicability() {
        ComplianceRule rule = new ComplianceRule();
        rule.setApplicablePlatforms(List.of("IOS-XE"));

        assertThat(checker.isApplicable(rule, "Cisco", "IOS-XE", "17.6")).isTrue();
        assertThat(checker.isApplicable(rule, "Cisco", "NX-OS", "9.3")).isFalse();
    }

    @Test
    @DisplayName("Null rule returns false")
    void testNullRule() {
        assertThat(checker.isApplicable(null, "Cisco", "IOS-XE", "17.6")).isFalse();
    }
}
