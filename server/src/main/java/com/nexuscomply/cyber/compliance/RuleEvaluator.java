package com.nexuscomply.cyber.compliance;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;

public interface RuleEvaluator {

    RuleEvaluationResult evaluate(CanonicalSecurityModel canonical, ComplianceRule rule);
}
