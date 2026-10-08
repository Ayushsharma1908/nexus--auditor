package com.nexuscomply.cyber.evidence;

import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.evidence.persistence.EvidenceDocument;
import com.nexuscomply.cyber.evidence.persistence.EvidenceRepository;
import com.nexuscomply.cyber.finding.FindingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EvidenceCreationServiceUnitTest {

    private EvidenceRepository evidenceRepository;
    private EvidenceCreationService evidenceCreationService;

    @BeforeEach
    void setUp() {
        evidenceRepository = mock(EvidenceRepository.class);
        evidenceCreationService = new EvidenceCreationServiceImpl(evidenceRepository);

        when(evidenceRepository.save(any(EvidenceDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Unit Test: EvidenceCreationService matches the correct SourceMapEntry out of multiple entries (picks the RIGHT one, not the first)")
    void testMatchesCorrectSourceMapEntryFromMultiple() {
        List<SourceMapEntry> sourceMap = List.of(
                new SourceMapEntry("security.ssh.version", 12, "ip ssh version 2"),
                new SourceMapEntry("logging.syslog", 18, "logging host 192.168.1.10"),
                new SourceMapEntry("security.telnet.enabled", 32, "transport input telnet ssh"),
                new SourceMapEntry("ntp.configured", 40, "ntp server 10.0.0.1")
        );

        RuleEvaluationResult result = RuleEvaluationResult.fail(
                "CIS-1.2",
                "CIS-1.2.2",
                false,
                true,
                "HIGH",
                "security.telnet.enabled",
                "Telnet is enabled on VTY lines"
        );

        FindingContext context = new FindingContext("audit-100", "dev-cisco-01", "cfg-200", "ver-300");
        String findingId = "finding-uuid-999";

        Optional<Evidence> evidenceOpt = evidenceCreationService.createEvidence(result, sourceMap, context, findingId);

        assertThat(evidenceOpt).isPresent();
        Evidence evidence = evidenceOpt.get();

        // Must match the EXACT canonicalField row (line 32), not the first row (line 12)
        assertThat(evidence.getSource()).isNotNull();
        assertThat(evidence.getSource().getLineNumber()).isEqualTo(32);
        assertThat(evidence.getSource().getRawText()).isEqualTo("transport input telnet ssh");
        assertThat(evidence.getSource().getSourceType()).isEqualTo("CONFIGURATION");

        // Canonical mapping verification
        assertThat(evidence.getCanonical()).isNotNull();
        assertThat(evidence.getCanonical().getField()).isEqualTo("security.telnet.enabled");
        assertThat(evidence.getCanonical().getValue()).isEqualTo(true);

        // Finding & traceability linkage
        assertThat(evidence.getFindingId()).isEqualTo("finding-uuid-999");
        assertThat(evidence.getAuditId()).isEqualTo("audit-100");
        assertThat(evidence.getConfigurationId()).isEqualTo("cfg-200");
        assertThat(evidence.getVersionId()).isEqualTo("ver-300");

        // Reason derivation
        assertThat(evidence.getReason()).isEqualTo("Configuration explicitly permits Telnet.");

        // Verify repository interaction
        ArgumentCaptor<EvidenceDocument> captor = ArgumentCaptor.forClass(EvidenceDocument.class);
        verify(evidenceRepository, times(1)).save(captor.capture());
        EvidenceDocument savedDoc = captor.getValue();
        assertThat(savedDoc.getSource().getLineNumber()).isEqualTo(32);
        assertThat(savedDoc.getSource().getRawText()).isEqualTo("transport input telnet ssh");
        assertThat(savedDoc.getVersionId()).isEqualTo("ver-300");
    }

    @Test
    @DisplayName("Unit Test: PASS rule evaluation result produces ZERO evidence")
    void testPassResultProducesZeroEvidence() {
        List<SourceMapEntry> sourceMap = List.of(
                new SourceMapEntry("security.telnet.enabled", 25, "transport input ssh")
        );

        RuleEvaluationResult passResult = RuleEvaluationResult.pass(
                "CIS-1.2",
                "CIS-1.2.2",
                false,
                false,
                "HIGH",
                "security.telnet.enabled"
        );

        FindingContext context = new FindingContext("audit-100", "dev-cisco-01", "cfg-200", "ver-300");

        Optional<Evidence> evidenceOpt = evidenceCreationService.createEvidence(passResult, sourceMap, context, "finding-uuid");

        assertThat(evidenceOpt).isEmpty();
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unit Test: Missing SourceMapEntry edge case is handled gracefully without throwing (Rule 3)")
    void testMissingSourceMapEntryHandledGracefully() {
        // Source map does not contain an entry for 'authentication.aaa'
        List<SourceMapEntry> sourceMap = List.of(
                new SourceMapEntry("security.ssh.version", 12, "ip ssh version 2"),
                new SourceMapEntry("logging.syslog", 18, "logging host 192.168.1.10")
        );

        RuleEvaluationResult result = RuleEvaluationResult.fail(
                "CIS-1.1",
                "CIS-1.1.1",
                true,
                false,
                "HIGH",
                "authentication.aaa",
                "AAA authentication is disabled"
        );

        FindingContext context = new FindingContext("audit-100", "dev-cisco-01", "cfg-200", "ver-trace-400");

        Optional<Evidence> evidenceOpt = evidenceCreationService.createEvidence(result, sourceMap, context, "finding-uuid-missing");

        assertThat(evidenceOpt).isPresent();
        Evidence evidence = evidenceOpt.get();

        // Must not throw, must have null lineNumber and descriptive text
        assertThat(evidence.getSource().getLineNumber()).isNull();
        assertThat(evidence.getSource().getRawText()).contains("No explicit configuration line observed");
        assertThat(evidence.getSource().getSourceType()).isEqualTo("CONFIGURATION");

        // Canonical details preserved
        assertThat(evidence.getCanonical().getField()).isEqualTo("authentication.aaa");
        assertThat(evidence.getCanonical().getValue()).isEqualTo(false);

        // Traceability preserved
        assertThat(evidence.getVersionId()).isEqualTo("ver-trace-400");
        assertThat(evidence.getFindingId()).isEqualTo("finding-uuid-missing");

        verify(evidenceRepository, times(1)).save(any(EvidenceDocument.class));
    }
}
