package com.nexuscomply.cyber.finding;

import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.compliance.RuleResultStatus;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.model.Control;
import com.nexuscomply.cyber.finding.persistence.FindingDocument;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FindingCreationServiceUnitTest {

    private FindingRepository findingRepository;
    private FindingCreationService findingCreationService;

    @BeforeEach
    void setUp() {
        findingRepository = mock(FindingRepository.class);
        findingCreationService = new FindingCreationServiceImpl(findingRepository);
    }

    @Test
    @DisplayName("Absolute Rule 1: PASS evaluation result produces ZERO findings")
    void testPassResultProducesZeroFindings() {
        RuleEvaluationResult passResult = new RuleEvaluationResult(
                RuleResultStatus.PASS,
                "ctrl-1.2.2",
                "CIS-1.2.2",
                false,
                false,
                "HIGH",
                "security.telnet.enabled",
                "Requirement satisfied"
        );
        FindingContext context = new FindingContext("audit-001", "dev-001", "cfg-001", "ver-001");

        Optional<Finding> findingOpt = findingCreationService.createFinding(passResult, context);

        assertThat(findingOpt).isEmpty();
        verify(findingRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = RuleResultStatus.class, names = {"PASS", "UNKNOWN", "NOT_APPLICABLE", "ERROR"})
    @DisplayName("Absolute Rule 1: Non-FAIL statuses (PASS, UNKNOWN, NOT_APPLICABLE, ERROR) never produce findings")
    void testNonFailStatusesNeverProduceFindings(RuleResultStatus status) {
        RuleEvaluationResult result = new RuleEvaluationResult(
                status,
                "ctrl-test",
                "RULE-TEST",
                true,
                null,
                "MEDIUM",
                "test.field",
                "Status: " + status
        );
        FindingContext context = new FindingContext("audit-001", "dev-001", "cfg-001", "ver-001");

        Optional<Finding> findingOpt = findingCreationService.createFinding(result, context);

        assertThat(findingOpt).isEmpty();
        verify(findingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Absolute Rule 1, 2, 4: FAIL result produces exactly ONE Finding with status OPEN and exact severity")
    void testFailResultProducesOneFindingWithStatusOpenAndExactSeverity() {
        RuleEvaluationResult failResult = new RuleEvaluationResult(
                RuleResultStatus.FAIL,
                "ctrl-1.2.2",
                "CIS-1.2.2",
                false,
                true,
                "HIGH",
                "security.telnet.enabled",
                "Expected [false] but found [true]"
        );

        ComplianceRule rule = new ComplianceRule();
        rule.setId("rule-uuid-122");
        rule.setRuleCode("CIS-1.2.2");
        rule.setControlId("ctrl-uuid-122");
        rule.setName("Telnet disabled on VTY");
        rule.setDescription("Insecure Telnet plaintext management protocol must be disabled");
        rule.setSeverity("HIGH");
        rule.setFrameworkIds(List.of("fw-cis-uuid"));

        Control control = new Control();
        control.setId("ctrl-uuid-122");
        control.setControlId("1.2.2");
        control.setTitle("Disable Telnet");

        FindingContext context = new FindingContext("audit-100", "dev-01", "cfg-55", "ver-2");

        when(findingRepository.save(any(FindingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Finding> findingOpt = findingCreationService.createFinding(failResult, rule, control, context);

        assertThat(findingOpt).isPresent();
        Finding finding = findingOpt.get();

        assertThat(finding.getStatus()).isEqualTo("OPEN");
        assertThat(finding.getComplianceStatus()).isEqualTo("FAIL");
        assertThat(finding.getSeverity()).isEqualTo("HIGH");
        assertThat(finding.getControlId()).isEqualTo("ctrl-uuid-122");
        assertThat(finding.getRuleId()).isEqualTo("rule-uuid-122");
        assertThat(finding.getControlCode()).isEqualTo("CIS-1.2.2");
        assertThat(finding.getTitle()).isEqualTo("Telnet enabled");
        assertThat(finding.getDescription()).isEqualTo("Telnet is enabled for management access.");
        assertThat(finding.getImpact()).contains("Insecure remote management protocol is enabled");
        assertThat(finding.getFrameworkIds()).containsExactly("fw-cis-uuid");
        assertThat(finding.getAuditId()).isEqualTo("audit-100");
        assertThat(finding.getDeviceId()).isEqualTo("dev-01");
        assertThat(finding.getConfigurationId()).isEqualTo("cfg-55");
        assertThat(finding.getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(finding.getExpected()).isEqualTo(false);
        assertThat(finding.getActual()).isEqualTo(true);
        assertThat(finding.getEvidenceIds()).isEmpty();
        assertThat(finding.isRemediationAvailable()).isTrue();

        ArgumentCaptor<FindingDocument> captor = ArgumentCaptor.forClass(FindingDocument.class);
        verify(findingRepository).save(captor.capture());
        FindingDocument savedDoc = captor.getValue();
        assertThat(savedDoc.getStatus()).isEqualTo("OPEN");
        assertThat(savedDoc.getSeverity()).isEqualTo("HIGH");
    }
}
