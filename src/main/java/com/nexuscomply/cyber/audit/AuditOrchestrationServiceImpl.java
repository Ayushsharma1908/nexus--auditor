package com.nexuscomply.cyber.audit;

import com.nexuscomply.cyber.audit.persistence.AuditDocument;
import com.nexuscomply.cyber.audit.persistence.AuditRepository;
import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.compliance.RuleApplicabilityChecker;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.compliance.RuleEvaluator;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.model.Control;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleDocument;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleRepository;
import com.nexuscomply.cyber.compliance.persistence.ControlDocument;
import com.nexuscomply.cyber.compliance.persistence.ControlRepository;
import com.nexuscomply.cyber.detection.VendorDetectionResponse;
import com.nexuscomply.cyber.detection.VendorDetectionService;
import com.nexuscomply.cyber.detection.VendorDetectionStatus;
import com.nexuscomply.cyber.finding.FindingContext;
import com.nexuscomply.cyber.finding.Finding;
import com.nexuscomply.cyber.finding.FindingCreationService;
import com.nexuscomply.cyber.normalization.NormalizationService;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.ParserService;
import com.nexuscomply.cyber.risk.RiskCalculationService;
import com.nexuscomply.cyber.risk.RiskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link AuditOrchestrationService}.
 *
 * <p>Coordinates the full 10-state audit lifecycle:
 * QUEUED -> DETECTING -> PARSING -> NORMALIZING -> UNKNOWN_REVIEW -> CHECKING -> RISK_CALCULATION -> COMPLETED
 *
 * <p>Enforces core rules:
 * <ul>
 *   <li>Updates status and timestamps in MongoDB at EVERY transition for real-time visibility.</li>
 *   <li>Exceptions in detect/parse/normalize transition audit to FAILED with diagnostic message.</li>
 *   <li>Rule checking errors (RuleResultStatus.ERROR) do not fail the audit; counted in summary.</li>
 *   <li>UNKNOWN_REVIEW is transient and non-blocking for this task (Decision 2).</li>
 *   <li>RISK_CALCULATION computes 5-factor risk per finding via {@link RiskCalculationService}.</li>
 *   <li>Refuses to cancel already-COMPLETED or already-FAILED audits.</li>
 *   <li>Evaluates against all active rules using {@link RuleApplicabilityChecker}.</li>
 *   <li>Calls {@link FindingCreationService} for FAIL results (creates Finding + Evidence).</li>
 * </ul>
 */
@Service
public class AuditOrchestrationServiceImpl implements AuditOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AuditOrchestrationServiceImpl.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?i)^\\s*version\\s+([0-9a-zA-Z._-]+)", Pattern.MULTILINE);

    private final AuditRepository auditRepository;
    private final VendorDetectionService vendorDetectionService;
    private final ParserService parserService;
    private final NormalizationService normalizationService;
    private final ComplianceRuleRepository complianceRuleRepository;
    private final ControlRepository controlRepository;
    private final RuleApplicabilityChecker ruleApplicabilityChecker;
    private final RuleEvaluator ruleEvaluator;
    private final FindingCreationService findingCreationService;
    private final RiskCalculationService riskCalculationService;
    private final com.nexuscomply.cyber.ai.service.AiMappingService aiMappingService;

    @Autowired
    public AuditOrchestrationServiceImpl(
            AuditRepository auditRepository,
            VendorDetectionService vendorDetectionService,
            ParserService parserService,
            NormalizationService normalizationService,
            ComplianceRuleRepository complianceRuleRepository,
            ControlRepository controlRepository,
            RuleApplicabilityChecker ruleApplicabilityChecker,
            RuleEvaluator ruleEvaluator,
            FindingCreationService findingCreationService,
            @Autowired(required = false) RiskCalculationService riskCalculationService,
            @Autowired(required = false) com.nexuscomply.cyber.ai.service.AiMappingService aiMappingService) {
        this.auditRepository = auditRepository;
        this.vendorDetectionService = vendorDetectionService;
        this.parserService = parserService;
        this.normalizationService = normalizationService;
        this.complianceRuleRepository = complianceRuleRepository;
        this.controlRepository = controlRepository;
        this.ruleApplicabilityChecker = ruleApplicabilityChecker;
        this.ruleEvaluator = ruleEvaluator;
        this.findingCreationService = findingCreationService;
        this.riskCalculationService = riskCalculationService;
        this.aiMappingService = aiMappingService;
    }

    public AuditOrchestrationServiceImpl(
            AuditRepository auditRepository,
            VendorDetectionService vendorDetectionService,
            ParserService parserService,
            NormalizationService normalizationService,
            ComplianceRuleRepository complianceRuleRepository,
            ControlRepository controlRepository,
            RuleApplicabilityChecker ruleApplicabilityChecker,
            RuleEvaluator ruleEvaluator,
            FindingCreationService findingCreationService,
            RiskCalculationService riskCalculationService) {
        this(auditRepository, vendorDetectionService, parserService, normalizationService,
                complianceRuleRepository, controlRepository, ruleApplicabilityChecker, ruleEvaluator,
                findingCreationService, riskCalculationService, null);
    }

    public AuditOrchestrationServiceImpl(
            AuditRepository auditRepository,
            VendorDetectionService vendorDetectionService,
            ParserService parserService,
            NormalizationService normalizationService,
            ComplianceRuleRepository complianceRuleRepository,
            ControlRepository controlRepository,
            RuleApplicabilityChecker ruleApplicabilityChecker,
            RuleEvaluator ruleEvaluator,
            FindingCreationService findingCreationService) {
        this(auditRepository, vendorDetectionService, parserService, normalizationService,
                complianceRuleRepository, controlRepository, ruleApplicabilityChecker, ruleEvaluator,
                findingCreationService, null, null);
    }

    @Override
    public Audit startAudit(String deviceId, String configurationId, String rawConfig) {
        return startAudit(deviceId, configurationId, UUID.randomUUID().toString(), rawConfig, null);
    }

    @Override
    public Audit startAudit(String deviceId, String configurationId, String versionId, String rawConfig) {
        return startAudit(deviceId, configurationId, versionId, rawConfig, null);
    }

    @Override
    public Audit startAudit(String deviceId, String configurationId, String versionId, String rawConfig, RiskContext riskContext) {
        Instant now = Instant.now();
        String auditId = UUID.randomUUID().toString();

        AuditDocument auditDoc = new AuditDocument();
        auditDoc.setId(auditId);
        auditDoc.setDeviceId(deviceId);
        auditDoc.setConfigurationId(configurationId);
        auditDoc.setVersionId(versionId);
        auditDoc.setStatus(AuditStatus.QUEUED.name());
        auditDoc.setProgress(new AuditProgress("QUEUED", 0));
        auditDoc.setStartedAt(now);
        auditDoc.setCreatedAt(now);
        auditDoc.setUpdatedAt(now);

        auditDoc = auditRepository.save(auditDoc);

        try {
            // Stage 1: DETECTING
            if (!updateProgress(auditDoc, AuditStatus.DETECTING, 10)) {
                return haltCancelled(auditId);
            }
            VendorDetectionResponse detection = vendorDetectionService.detectVendor(rawConfig);
            if (detection == null || detection.getStatus() == VendorDetectionStatus.UNKNOWN
                    || detection.getStatus() == VendorDetectionStatus.UNCERTAIN
                    || "UNKNOWN".equalsIgnoreCase(detection.getVendor())) {
                return failAudit(auditDoc, "Vendor detection failed: vendor and platform could not be identified from configuration syntax with sufficient confidence (status: "
                        + (detection != null ? detection.getStatus() : "null") + ").");
            }

            String vendor = detection.getVendor();
            String platform = detection.getPlatform();
            double detectionConfidence = detection.getConfidence();
            String osVersion = extractOsVersion(rawConfig);

            // Stage 2: PARSING
            if (!updateProgress(auditDoc, AuditStatus.PARSING, 25)) {
                return haltCancelled(auditId);
            }
            ParserResult parserResult = parserService.parse(rawConfig, vendor, platform);
            if (parserResult == null || "FAILED".equalsIgnoreCase(parserResult.getStatus())) {
                return failAudit(auditDoc, "Parser execution failed for vendor [" + vendor + "] and platform [" + platform + "].");
            }

            // Stage 3: NORMALIZING
            if (!updateProgress(auditDoc, AuditStatus.NORMALIZING, 45)) {
                return haltCancelled(auditId);
            }
            NormalizedConfigurationDocument normalizedDoc = normalizationService.normalizeAndPersist(
                    rawConfig, deviceId, configurationId, versionId, vendor, platform, osVersion
            );
            if (normalizedDoc == null) {
                return failAudit(auditDoc, "Normalization failed to generate canonical configuration document.");
            }
            auditDoc.setNormalizedConfigurationId(normalizedDoc.getId());
            auditDoc = auditRepository.save(auditDoc);

            CanonicalSecurityModel canonical = normalizedDoc.getCanonical();
            List<SourceMapEntry> sourceMap = normalizedDoc.getSourceMap();

            // Stage 4: UNKNOWN_REVIEW (Transient, non-blocking per Pre-Resolved Decision 2)
            int unknownCount = parserResult.getUnknowns() != null ? parserResult.getUnknowns().size() : 0;
            log.info("Audit [{}]: Transitioning through UNKNOWN_REVIEW (found {} unknown syntax items).", auditId, unknownCount);
            if (!updateProgress(auditDoc, AuditStatus.UNKNOWN_REVIEW, 60)) {
                return haltCancelled(auditId);
            }
            if (aiMappingService != null && parserResult.getUnknowns() != null) {
                for (com.nexuscomply.cyber.parser.UnknownConstruct unknown : parserResult.getUnknowns()) {
                    if (unknown != null && unknown.getRawText() != null && !unknown.getRawText().trim().isEmpty()) {
                        try {
                            aiMappingService.recordUnknownSyntax(vendor, platform, unknown.getRawText().trim());
                        } catch (Exception ex) {
                            log.warn("Failed to record unknown syntax [{}]: {}", unknown.getRawText(), ex.getMessage());
                        }
                    }
                }
            }

            // Stage 5: CHECKING
            if (!updateProgress(auditDoc, AuditStatus.CHECKING, 75)) {
                return haltCancelled(auditId);
            }
            List<ComplianceRuleDocument> activeRuleDocs = complianceRuleRepository.findAll().stream()
                    .filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus()))
                    .toList();

            int totalControls = activeRuleDocs.size();
            int passed = 0;
            int failed = 0;
            int unknown = 0;
            int notApplicable = 0;
            int error = 0;

            Set<String> frameworkIds = new LinkedHashSet<>();
            FindingContext findingContext = new FindingContext(auditId, deviceId, configurationId, versionId, sourceMap);
            List<FindingRulePair> createdFindings = new ArrayList<>();

            for (ComplianceRuleDocument ruleDoc : activeRuleDocs) {
                if (isCancelled(auditId)) {
                    return haltCancelled(auditId);
                }
                ComplianceRule domainRule = toDomainRule(ruleDoc);

                if (ruleDoc.getFrameworkIds() != null) {
                    frameworkIds.addAll(ruleDoc.getFrameworkIds());
                }

                // Use the EXISTING RuleApplicabilityChecker per Pre-Resolved Decision 4
                if (!ruleApplicabilityChecker.isApplicable(domainRule, vendor, platform, osVersion)) {
                    notApplicable++;
                    continue;
                }

                try {
                    RuleEvaluationResult evalResult = ruleEvaluator.evaluate(canonical, domainRule);
                    if (evalResult == null) {
                        error++;
                        continue;
                    }

                    switch (evalResult.getStatus()) {
                        case PASS -> passed++;
                        case FAIL -> {
                            failed++;
                            Control domainControl = null;
                            if (ruleDoc.getControlId() != null) {
                                Optional<ControlDocument> ctrlOpt = controlRepository.findById(ruleDoc.getControlId());
                                if (ctrlOpt.isPresent()) {
                                    domainControl = toDomainControl(ctrlOpt.get());
                                }
                            }
                            Optional<Finding> findingOpt = findingCreationService.createFinding(evalResult, domainRule, domainControl, findingContext);
                            findingOpt.ifPresent(f -> createdFindings.add(new FindingRulePair(f, domainRule)));
                        }
                        case UNKNOWN -> unknown++;
                        case NOT_APPLICABLE -> notApplicable++;
                        case ERROR -> error++;
                    }
                } catch (Exception evalEx) {
                    // Absolute Rule 2: Rule evaluation errors do not fail the whole audit
                    log.warn("Error evaluating rule [{}]: {}", domainRule.getRuleCode(), evalEx.getMessage());
                    error++;
                }
            }

            // Stage 6: RISK_CALCULATION
            log.info("Audit [{}]: Transitioning through RISK_CALCULATION with {} finding(s).", auditId, createdFindings.size());
            if (!updateProgress(auditDoc, AuditStatus.RISK_CALCULATION, 90)) {
                return haltCancelled(auditId);
            }

            if (isCancelled(auditId)) {
                return haltCancelled(auditId);
            }

            if (riskCalculationService != null && !createdFindings.isEmpty()) {
                RiskContext effectiveRiskContext;
                if (riskContext != null) {
                    effectiveRiskContext = riskContext;
                    effectiveRiskContext.setDetectionConfidence(detectionConfidence);
                } else {
                    effectiveRiskContext = new RiskContext(detectionConfidence);
                }

                for (FindingRulePair pair : createdFindings) {
                    try {
                        riskCalculationService.calculateAndPersistRisk(pair.finding(), pair.rule(), effectiveRiskContext);
                    } catch (Exception riskEx) {
                        log.warn("Error calculating risk for finding [{}]: {}", pair.finding().getId(), riskEx.getMessage());
                    }
                }
            }

            // Stage 7: COMPLETED
            AuditSummary summary = new AuditSummary(totalControls, passed, failed, unknown, notApplicable, error);
            auditDoc.setSummary(summary);
            auditDoc.setFrameworkIds(new ArrayList<>(frameworkIds));

            int applicableControls = totalControls - notApplicable;
            // Compute complianceScore = (passed / applicableControls) * 100.0 (rounded to 1 decimal place).
            // When no control is PASS/FAIL (or applicableControls == 0), score is left null
            // so dashboards and downstream consumers distinguish "clean pass" from "zero rules evaluated".
            Double complianceScore = (passed + failed > 0 && applicableControls > 0)
                    ? Math.round(((double) passed / (double) applicableControls) * 1000.0) / 10.0
                    : null;
            auditDoc.setComplianceScore(complianceScore);

            auditDoc.setStatus(AuditStatus.COMPLETED.name());
            auditDoc.setProgress(new AuditProgress("COMPLETED", 100));
            auditDoc.setCompletedAt(Instant.now());
            auditDoc.setUpdatedAt(Instant.now());

            AuditDocument completed = auditRepository.save(auditDoc);
            return toDomain(completed);

        } catch (Exception ex) {
            log.error("Fatal exception during audit execution [{}]: {}", auditId, ex.getMessage(), ex);
            return failAudit(auditDoc, "Audit failed due to error: " + ex.getMessage());
        }
    }

    @Override
    public boolean cancel(String auditId) {
        if (auditId == null) return false;
        Optional<AuditDocument> auditDocOpt = auditRepository.findById(auditId);
        if (auditDocOpt.isEmpty()) {
            return false;
        }

        AuditDocument doc = auditDocOpt.get();
        String currentStatus = doc.getStatus();

        // Absolute Rule 4: Refuse to cancel if already COMPLETED or FAILED
        if (AuditStatus.COMPLETED.name().equalsIgnoreCase(currentStatus)
                || AuditStatus.FAILED.name().equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Cannot cancel an audit in terminal state: " + currentStatus);
        }

        if (AuditStatus.CANCELLED.name().equalsIgnoreCase(currentStatus)) {
            return true;
        }

        doc.setStatus(AuditStatus.CANCELLED.name());
        doc.setProgress(new AuditProgress("CANCELLED", doc.getProgress() != null ? doc.getProgress().getPercent() : 0));
        doc.setCompletedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        auditRepository.save(doc);
        return true;
    }

    @Override
    public Optional<Audit> getAudit(String auditId) {
        if (auditId == null) return Optional.empty();
        return auditRepository.findById(auditId).map(this::toDomain);
    }

    private java.util.function.Consumer<AuditDocument> stageTransitionHook;

    public void setStageTransitionHook(java.util.function.Consumer<AuditDocument> stageTransitionHook) {
        this.stageTransitionHook = stageTransitionHook;
    }

    private boolean updateProgress(AuditDocument doc, AuditStatus status, int percent) {
        if (isCancelled(doc.getId())) {
            return false;
        }
        doc.setStatus(status.name());
        doc.setProgress(new AuditProgress(status.name(), percent));
        doc.setUpdatedAt(Instant.now());
        auditRepository.save(doc);
        if (stageTransitionHook != null) {
            stageTransitionHook.accept(doc);
        }
        return !isCancelled(doc.getId());
    }

    private boolean isCancelled(String auditId) {
        if (auditId == null) return false;
        return auditRepository.findById(auditId)
                .map(d -> AuditStatus.CANCELLED.name().equalsIgnoreCase(d.getStatus()))
                .orElse(false);
    }

    private Audit haltCancelled(String auditId) {
        log.info("Audit [{}] was cancelled externally. Halting pipeline execution.", auditId);
        return auditRepository.findById(auditId).map(this::toDomain).orElse(null);
    }

    private Audit failAudit(AuditDocument doc, String diagnosticMessage) {
        doc.setStatus(AuditStatus.FAILED.name());
        doc.setErrorMessage(diagnosticMessage);
        doc.setCompletedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        AuditDocument saved = auditRepository.save(doc);
        return toDomain(saved);
    }

    String extractOsVersion(String rawConfig) {
        if (rawConfig == null) return "UNKNOWN";
        Matcher matcher = VERSION_PATTERN.matcher(rawConfig);
        if (matcher.find()) {
            String ver = matcher.group(1);
            if (ver.startsWith("17.")) {
                return "17.x";
            }
            return ver;
        }
        return "UNKNOWN";
    }

    ComplianceRule toDomainRule(ComplianceRuleDocument doc) {
        ComplianceRule rule = new ComplianceRule();
        rule.setId(doc.getId());
        rule.setRuleCode(doc.getRuleCode());
        rule.setControlId(doc.getControlId());
        rule.setName(doc.getName());
        rule.setDescription(doc.getDescription());
        rule.setRequirement(doc.getExpression());
        rule.setSeverity(doc.getSeverity());
        rule.setFrameworkIds(doc.getFrameworkIds());
        rule.setApplicableVendors(doc.getApplicableVendors());
        rule.setApplicablePlatforms(doc.getApplicablePlatforms());
        rule.setApplicableOsVersions(doc.getApplicableOsVersions());
        rule.setStatus(doc.getStatus());
        rule.setVersion(doc.getVersion());
        return rule;
    }

    private Control toDomainControl(ControlDocument doc) {
        Control ctrl = new Control();
        ctrl.setId(doc.getId());
        ctrl.setControlId(doc.getControlId());
        ctrl.setTitle(doc.getTitle());
        ctrl.setDescription(doc.getDescription());
        ctrl.setCategory(doc.getCategory());
        ctrl.setSeverity(doc.getSeverity());
        ctrl.setStatus(doc.getStatus());
        return ctrl;
    }

    private Audit toDomain(AuditDocument doc) {
        Audit audit = new Audit();
        audit.setId(doc.getId());
        audit.setDeviceId(doc.getDeviceId());
        audit.setConfigurationId(doc.getConfigurationId());
        audit.setVersionId(doc.getVersionId());
        audit.setNormalizedConfigurationId(doc.getNormalizedConfigurationId());
        audit.setFrameworkIds(doc.getFrameworkIds());
        audit.setStatus(doc.getStatus());
        audit.setProgress(doc.getProgress());
        audit.setSummary(doc.getSummary());
        audit.setComplianceScore(doc.getComplianceScore());
        audit.setStartedAt(doc.getStartedAt());
        audit.setCompletedAt(doc.getCompletedAt());
        audit.setCreatedBy(doc.getCreatedBy());
        audit.setErrorMessage(doc.getErrorMessage());
        audit.setCreatedAt(doc.getCreatedAt());
        audit.setUpdatedAt(doc.getUpdatedAt());
        return audit;
    }

    private record FindingRulePair(Finding finding, ComplianceRule rule) {}
}
