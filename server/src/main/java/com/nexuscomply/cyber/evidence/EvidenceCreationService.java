package com.nexuscomply.cyber.evidence;

import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.finding.FindingContext;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for creating and persisting technical Evidence supporting compliance findings.
 */
public interface EvidenceCreationService {

    /**
     * Creates and persists an Evidence record for a FAIL evaluation result using the matching
     * SourceMapEntry from the normalized configuration source map.
     *
     * @param result the rule evaluation result (must have status FAIL)
     * @param sourceMap the source map entries from the parsed/normalized configuration
     * @param context contextual metadata (auditId, deviceId, configurationId, versionId)
     * @param findingId the ID of the finding this evidence supports
     * @return Optional containing the persisted Evidence domain object if result is FAIL, otherwise Optional.empty()
     */
    Optional<Evidence> createEvidence(RuleEvaluationResult result, List<SourceMapEntry> sourceMap, FindingContext context, String findingId);

    /**
     * Convenience method to create evidence and return its persisted document ID.
     *
     * @param result the rule evaluation result
     * @param sourceMap the source map entries
     * @param context contextual metadata
     * @param findingId the finding ID
     * @return the ID of the persisted Evidence document, or null if not created
     */
    default String createAndPersistEvidence(RuleEvaluationResult result, List<SourceMapEntry> sourceMap, FindingContext context, String findingId) {
        return createEvidence(result, sourceMap, context, findingId)
                .map(Evidence::getId)
                .orElse(null);
    }
}
