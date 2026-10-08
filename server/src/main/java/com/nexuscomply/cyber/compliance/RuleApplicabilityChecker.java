package com.nexuscomply.cyber.compliance;

import com.nexuscomply.cyber.compliance.model.ComplianceRule;

public interface RuleApplicabilityChecker {

    boolean isApplicable(ComplianceRule rule, String vendor, String platform, String osVersion);
}
