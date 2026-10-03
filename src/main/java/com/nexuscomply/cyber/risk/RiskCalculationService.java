package com.nexuscomply.cyber.risk;

import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.finding.Finding;

/**
 * Service for calculating contextual risk scores for compliance findings.
 *
 * <p>Implements the 5-factor risk formula from cyberlayer.pdf section 18:
 * 1. Finding Severity (35% weight)
 * 2. Asset Criticality (20% weight)
 * 3. Network Exposure (20% weight)
 * 4. Control Importance / Weight (15% weight, derived from rule severity proxy)
 * 5. Confidence / Uncertainty (10% weight, real vendor detection confidence)
 *
 * <p>Risk score is calculated PER FINDING. Finding.severity is NEVER modified.
 */
public interface RiskCalculationService {

    /**
     * Calculates the 5-factor risk score for a finding and persists the RiskAssessment document.
     *
     * @param finding the non-compliant finding
     * @param context the contextual metadata (asset criticality, network exposure, detection confidence)
     * @return the persisted RiskAssessment domain object
     */
    RiskAssessment calculateAndPersistRisk(Finding finding, RiskContext context);

    /**
     * Calculates the 5-factor risk score for a finding and persists the RiskAssessment document,
     * using the explicit originating compliance rule for control weight derivation.
     *
     * @param finding the non-compliant finding
     * @param rule the originating compliance rule
     * @param context the contextual metadata
     * @return the persisted RiskAssessment domain object
     */
    RiskAssessment calculateAndPersistRisk(Finding finding, ComplianceRule rule, RiskContext context);

    /**
     * Pure calculation without persistence (useful for evaluation, preview, and unit tests).
     *
     * @param finding the finding
     * @param context the contextual metadata
     * @return the in-memory RiskAssessment domain object
     */
    RiskAssessment calculateRisk(Finding finding, RiskContext context);

    /**
     * Pure calculation without persistence with explicit rule.
     *
     * @param finding the finding
     * @param rule the originating compliance rule
     * @param context the contextual metadata
     * @return the in-memory RiskAssessment domain object
     */
    RiskAssessment calculateRisk(Finding finding, ComplianceRule rule, RiskContext context);
}
