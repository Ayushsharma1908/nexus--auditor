package com.nexuscomply.cyber.evidence;

import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.compliance.RuleResultStatus;
import com.nexuscomply.cyber.evidence.persistence.EvidenceDocument;
import com.nexuscomply.cyber.evidence.persistence.EvidenceRepository;
import com.nexuscomply.cyber.finding.FindingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link EvidenceCreationService}.
 *
 * <p>Enforces core rules:
 * <ul>
 *   <li>Only creates evidence for FAIL rule evaluation results. PASS produces zero evidence.</li>
 *   <li>Matches SourceMapEntry by EXACT canonicalField equality (Absolute Rule 2).</li>
 *   <li>Gracefully handles missing SourceMapEntry without throwing (Absolute Rule 3).</li>
 *   <li>Preserves versionId traceability from FindingContext (Absolute Rule 4).</li>
 *   <li>Field names strictly match schema1.md section 11.</li>
 * </ul>
 */
@Service("cyberEvidenceCreationService")
public class EvidenceCreationServiceImpl implements EvidenceCreationService {

    private final EvidenceRepository evidenceRepository;

    @Autowired
    public EvidenceCreationServiceImpl(EvidenceRepository evidenceRepository) {
        this.evidenceRepository = evidenceRepository;
    }

    @Override
    public Optional<Evidence> createEvidence(
            RuleEvaluationResult result,
            List<SourceMapEntry> sourceMap,
            FindingContext context,
            String findingId) {

        // Rule: Only FAIL evaluation results produce evidence. PASS, UNKNOWN, etc. produce zero evidence.
        if (result == null || result.getStatus() != RuleResultStatus.FAIL) {
            return Optional.empty();
        }

        String canonicalField = result.getEvidenceSourceField();

        // Exact match on canonicalField per Absolute Rule 2
        SourceMapEntry matchedEntry = null;
        if (sourceMap != null && canonicalField != null) {
            for (SourceMapEntry entry : sourceMap) {
                if (entry != null && canonicalField.equals(entry.getCanonicalField())) {
                    matchedEntry = entry;
                    break;
                }
            }
        }

        Instant now = Instant.now();
        EvidenceDocument doc = new EvidenceDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setFindingId(findingId);

        if (context != null) {
            doc.setAuditId(context.auditId());
            doc.setConfigurationId(context.configurationId());
            doc.setVersionId(context.versionId()); // Traceability per Absolute Rule 4
        }

        // Source mapping per schema1.md section 11 and Absolute Rules 1 & 3
        EvidenceSource source = new EvidenceSource();
        if (matchedEntry != null && matchedEntry.getSourceType() != null) {
            source.setSourceType(matchedEntry.getSourceType());
        } else {
            source.setSourceType("CONFIGURATION");
        }

        if (matchedEntry != null) {
            source.setLineNumber(matchedEntry.getSourceLine());
            source.setRawText(matchedEntry.getRawText());
        } else {
            // Missing SourceMapEntry handled defensively per Absolute Rule 3: null line number, explicit note
            source.setLineNumber(null);
            source.setRawText("No explicit configuration line observed (inferred from default or absent setting)");
        }
        doc.setSource(source);

        // Canonical mapping per schema1.md section 11
        EvidenceCanonical canonical = new EvidenceCanonical();
        canonical.setField(canonicalField);
        canonical.setValue(result.getActual());
        doc.setCanonical(canonical);

        // Reason derivation matching schema1.md section 11 example style ("Configuration explicitly permits Telnet.")
        String reason = deriveReason(result, matchedEntry, canonicalField);
        doc.setReason(reason);

        doc.setCreatedAt(now);

        EvidenceDocument saved = evidenceRepository.save(doc);
        return Optional.of(toDomain(saved));
    }

    private String deriveReason(RuleEvaluationResult result, SourceMapEntry matchedEntry, String canonicalField) {
        if ("security.telnet.enabled".equals(canonicalField) && Boolean.TRUE.equals(result.getActual())) {
            return "Configuration explicitly permits Telnet.";
        }
        if (matchedEntry == null) {
            return (result.getMessage() != null && !result.getMessage().isBlank())
                    ? result.getMessage() + " (no explicit configuration line observed in source map)"
                    : "No matching source configuration line observed in source map for canonical field: " + canonicalField;
        }
        if (result.getMessage() != null && !result.getMessage().isBlank()) {
            return result.getMessage();
        }
        return "Non-compliant configuration observed: " + matchedEntry.getRawText();
    }

    private Evidence toDomain(EvidenceDocument doc) {
        Evidence evidence = new Evidence();
        evidence.setId(doc.getId());
        evidence.setFindingId(doc.getFindingId());
        evidence.setAuditId(doc.getAuditId());
        evidence.setConfigurationId(doc.getConfigurationId());
        evidence.setVersionId(doc.getVersionId());
        evidence.setSource(doc.getSource());
        evidence.setCanonical(doc.getCanonical());
        evidence.setReason(doc.getReason());
        evidence.setCreatedAt(doc.getCreatedAt());
        return evidence;
    }
}
