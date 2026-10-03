package com.nexuscomply.cyber.finding;

import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.model.Control;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for creating and persisting compliance findings.
 *
 * <p>Absolute rule: A Finding is created ONLY when a rule evaluation result has status FAIL.
 * PASS, UNKNOWN, NOT_APPLICABLE, and ERROR never create findings.
 */
public interface FindingCreationService {

    /**
     * Creates and persists a Finding if and only if the rule evaluation result is FAIL.
     *
     * @param result the evaluation result
     * @param rule the originating compliance rule
     * @param control the originating control (may be null)
     * @param context contextual metadata (auditId, deviceId, configurationId, versionId)
     * @return Optional containing the persisted Finding if status is FAIL, otherwise Optional.empty()
     */
    Optional<Finding> createFinding(RuleEvaluationResult result, ComplianceRule rule, Control control, FindingContext context);

    /**
     * Creates and persists a Finding if and only if the rule evaluation result is FAIL,
     * automatically looking up the rule and control from repositories by ruleId / ruleCode.
     *
     * @param result the evaluation result
     * @param context contextual metadata (auditId, deviceId, configurationId, versionId)
     * @return Optional containing the persisted Finding if status is FAIL, otherwise Optional.empty()
     */
    Optional<Finding> createFinding(RuleEvaluationResult result, FindingContext context);

    /**
     * Creates and persists a Finding with an explicit source map.
     */
    default Optional<Finding> createFinding(RuleEvaluationResult result, ComplianceRule rule, Control control, FindingContext context, List<SourceMapEntry> sourceMap) {
        FindingContext enrichedContext = (context != null)
                ? new FindingContext(context.auditId(), context.deviceId(), context.configurationId(), context.versionId(), sourceMap)
                : new FindingContext(null, null, null, null, sourceMap);
        return createFinding(result, rule, control, enrichedContext);
    }

    /**
     * Creates and persists a Finding with an explicit source map.
     */
    default Optional<Finding> createFinding(RuleEvaluationResult result, FindingContext context, List<SourceMapEntry> sourceMap) {
        FindingContext enrichedContext = (context != null)
                ? new FindingContext(context.auditId(), context.deviceId(), context.configurationId(), context.versionId(), sourceMap)
                : new FindingContext(null, null, null, null, sourceMap);
        return createFinding(result, enrichedContext);
    }
}
