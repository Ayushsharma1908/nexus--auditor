package com.nexuscomply.cyber.risk;

import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.finding.Finding;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class RiskCalculationServiceTest {

    private RiskAssessmentRepository riskAssessmentRepository;
    private RiskCalculationService riskCalculationService;

    @BeforeEach
    void setUp() {
        riskAssessmentRepository = Mockito.mock(RiskAssessmentRepository.class);
        // Mock save to return whatever is passed in
        Mockito.when(riskAssessmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        riskCalculationService = new RiskCalculationServiceImpl(riskAssessmentRepository);
    }

    @Test
    @DisplayName("Hand-verifiable calculation: CRITICAL severity + CRITICAL criticality + EXTERNAL exposure + 1.0 confidence -> 100 (CRITICAL)")
    void testTopScaleCriticalRiskCalculation() {
        Finding finding = createTestFinding("CRITICAL");
        ComplianceRule rule = createTestRule("CRITICAL");
        RiskContext context = new RiskContext("CRITICAL", "EXTERNAL", 1.0);

        RiskAssessment assessment = riskCalculationService.calculateRisk(finding, rule, context);

        assertThat(assessment).isNotNull();
        // Math breakdown:
        // Severity (100 * 0.35) = 35.0
        // Asset Criticality (100 * 0.20) = 20.0
        // Network Exposure (100 * 0.20) = 20.0
        // Control Weight (100 * 0.15) = 15.0
        // Confidence (100 * 0.10) = 10.0
        // Total = 100.0 -> Score 100
        assertThat(assessment.getScore()).isEqualTo(100);
        assertThat(assessment.getLevel()).isEqualTo("CRITICAL");

        RiskFactors factors = assessment.getFactors();
        assertThat(factors).isNotNull();
        assertThat(factors.getSeverity()).isEqualTo(10.0);
        assertThat(factors.getAssetCriticality()).isEqualTo(10.0);
        assertThat(factors.getExposure()).isEqualTo(10.0);
        assertThat(factors.getExploitability()).isEqualTo(10.0);
        assertThat(factors.getConfidence()).isEqualTo(10.0);

        Map<String, Double> contribs = factors.getContributions();
        assertThat(contribs.get("severity")).isEqualTo(35.0);
        assertThat(contribs.get("assetCriticality")).isEqualTo(20.0);
        assertThat(contribs.get("exposure")).isEqualTo(20.0);
        assertThat(contribs.get("exploitability")).isEqualTo(15.0);
        assertThat(contribs.get("confidence")).isEqualTo(10.0);

        // Verify Finding severity is NEVER touched
        assertThat(finding.getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Hand-verifiable calculation: LOW severity + LOW criticality + ISOLATED exposure + 0.10 confidence -> 19 (LOW)")
    void testBottomScaleLowRiskCalculation() {
        Finding finding = createTestFinding("LOW");
        ComplianceRule rule = createTestRule("LOW");
        RiskContext context = new RiskContext("LOW", "ISOLATED", 0.10);

        RiskAssessment assessment = riskCalculationService.calculateRisk(finding, rule, context);

        assertThat(assessment).isNotNull();
        // Math breakdown:
        // Severity (20 * 0.35) = 7.0
        // Asset Criticality (20 * 0.20) = 4.0
        // Network Exposure (20 * 0.20) = 4.0
        // Control Weight / Exploitability (20 * 0.15) = 3.0
        // Confidence (10 * 0.10) = 1.0
        // Total = 19.0 -> Score 19
        assertThat(assessment.getScore()).isEqualTo(19);
        assertThat(assessment.getLevel()).isEqualTo("LOW");

        RiskFactors factors = assessment.getFactors();
        assertThat(factors.getSeverity()).isEqualTo(2.0);
        assertThat(factors.getAssetCriticality()).isEqualTo(2.0);
        assertThat(factors.getExposure()).isEqualTo(2.0);
        assertThat(factors.getExploitability()).isEqualTo(2.0);
        assertThat(factors.getConfidence()).isEqualTo(1.0);

        Map<String, Double> contribs = factors.getContributions();
        assertThat(contribs.get("severity")).isEqualTo(7.0);
        assertThat(contribs.get("assetCriticality")).isEqualTo(4.0);
        assertThat(contribs.get("exposure")).isEqualTo(4.0);
        assertThat(contribs.get("exploitability")).isEqualTo(3.0);
        assertThat(contribs.get("confidence")).isEqualTo(1.0);

        // Verify Finding severity is NEVER touched
        assertThat(finding.getSeverity()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Hand-verifiable calculation: HIGH severity + DEFAULT placeholders (MEDIUM/INTERNAL) + 0.99 Cisco confidence -> 70 (HIGH)")
    void testDefaultPlaceholderTelnetRiskCalculation() {
        Finding finding = createTestFinding("HIGH");
        ComplianceRule rule = createTestRule("HIGH");
        RiskContext context = new RiskContext(0.99); // defaults assetCriticality and networkExposure

        RiskAssessment assessment = riskCalculationService.calculateRisk(finding, rule, context);

        assertThat(assessment).isNotNull();
        // Math breakdown:
        // Severity: HIGH (80 * 0.35) = 28.0
        // Asset Criticality: default MEDIUM (50 * 0.20) = 10.0
        // Network Exposure: default INTERNAL (50 * 0.20) = 10.0
        // Control Weight / Exploitability: HIGH (80 * 0.15) = 12.0
        // Confidence: 0.99 (99.0 * 0.10) = 9.9
        // Total = 28.0 + 10.0 + 10.0 + 12.0 + 9.9 = 69.9 -> Math.round = 70
        assertThat(assessment.getScore()).isEqualTo(70);
        assertThat(assessment.getLevel()).isEqualTo("HIGH");

        assertThat(assessment.getAssetCriticalitySource()).isEqualTo(RiskContext.SOURCE_DEFAULT_PLACEHOLDER);
        assertThat(assessment.getNetworkExposureSource()).isEqualTo(RiskContext.SOURCE_DEFAULT_PLACEHOLDER);
        assertThat(assessment.getConfidence()).isEqualTo(0.99);
        assertThat(assessment.getConfidenceSource()).isEqualTo("VENDOR_DETECTION");

        Map<String, Object> rawInputs = assessment.getFactors().getRawInputs();
        assertThat(rawInputs.get("severity")).isEqualTo("HIGH");
        assertThat(rawInputs.get("assetCriticality")).isEqualTo("MEDIUM");
        assertThat(rawInputs.get("networkExposure")).isEqualTo("INTERNAL");
        assertThat(rawInputs.get("exploitability")).isEqualTo("HIGH");
        assertThat(rawInputs.get("confidence")).isEqualTo(0.99);
    }

    @Test
    @DisplayName("Verification of default placeholder vs provided input flagging")
    void testSourceFlaggingDifferentiatesProvidedFromDefault() {
        Finding finding = createTestFinding("HIGH");

        // Case A: Default placeholders
        RiskContext defaultContext = new RiskContext(0.85);
        RiskAssessment defaultAssessment = riskCalculationService.calculateRisk(finding, defaultContext);
        assertThat(defaultAssessment.getAssetCriticalitySource()).isEqualTo("DEFAULT_PLACEHOLDER");
        assertThat(defaultAssessment.getNetworkExposureSource()).isEqualTo("DEFAULT_PLACEHOLDER");

        // Case B: Explicitly provided values
        RiskContext providedContext = new RiskContext("HIGH", "DMZ", 0.85);
        RiskAssessment providedAssessment = riskCalculationService.calculateRisk(finding, providedContext);
        assertThat(providedAssessment.getAssetCriticalitySource()).isEqualTo("PROVIDED");
        assertThat(providedAssessment.getNetworkExposureSource()).isEqualTo("PROVIDED");
    }

    private Finding createTestFinding(String severity) {
        Finding finding = new Finding();
        finding.setId(UUID.randomUUID().toString());
        finding.setAuditId("audit-test-uuid");
        finding.setDeviceId("device-test-uuid");
        finding.setConfigurationId("cfg-test-uuid");
        finding.setRuleId("rule-test-uuid");
        finding.setTitle("Telnet enabled");
        finding.setSeverity(severity);
        finding.setComplianceStatus("FAIL");
        return finding;
    }

    private ComplianceRule createTestRule(String severity) {
        ComplianceRule rule = new ComplianceRule();
        rule.setId(UUID.randomUUID().toString());
        rule.setRuleCode("TEST-RULE-01");
        rule.setSeverity(severity);
        return rule;
    }
}
