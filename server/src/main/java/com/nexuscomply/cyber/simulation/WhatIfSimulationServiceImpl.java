package com.nexuscomply.cyber.simulation;

import com.nexuscomply.configuration.model.Configuration;
import com.nexuscomply.configuration.repository.ConfigurationRepository;
import com.nexuscomply.cyber.ai.service.CanonicalFieldAllowlist;
import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.compliance.RuleApplicabilityChecker;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.compliance.RuleEvaluator;
import com.nexuscomply.cyber.compliance.RuleResultStatus;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleDocument;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleRepository;
import com.nexuscomply.cyber.compliance.persistence.ControlRepository;
import com.nexuscomply.cyber.drift.model.DriftClassification;
import com.nexuscomply.cyber.finding.Finding;
import com.nexuscomply.cyber.finding.persistence.FindingDocument;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.ParserService;
import com.nexuscomply.cyber.risk.RiskAssessment;
import com.nexuscomply.cyber.risk.RiskCalculationService;
import com.nexuscomply.cyber.risk.RiskContext;
import com.nexuscomply.device.model.Device;
import com.nexuscomply.device.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service("cyberWhatIfSimulationService")
public class WhatIfSimulationServiceImpl implements WhatIfSimulationService {

    private static final Logger log = LoggerFactory.getLogger(WhatIfSimulationServiceImpl.class);

    private final NormalizedConfigurationRepository normalizedConfigRepository;
    private final ParserService parserService;
    private final ComplianceRuleRepository complianceRuleRepository;
    private final ControlRepository controlRepository;
    private final RuleApplicabilityChecker ruleApplicabilityChecker;
    private final RuleEvaluator ruleEvaluator;
    private final RiskCalculationService riskCalculationService;
    private final FindingRepository findingRepository;
    private final WhatIfSimulationRepository simulationRepository;
    private final DeviceRepository deviceRepository;
    private final ConfigurationRepository configurationRepository;

    @Autowired
    public WhatIfSimulationServiceImpl(
            NormalizedConfigurationRepository normalizedConfigRepository,
            ParserService parserService,
            ComplianceRuleRepository complianceRuleRepository,
            ControlRepository controlRepository,
            RuleApplicabilityChecker ruleApplicabilityChecker,
            RuleEvaluator ruleEvaluator,
            RiskCalculationService riskCalculationService,
            @Autowired(required = false) FindingRepository findingRepository,
            @Autowired(required = false) WhatIfSimulationRepository simulationRepository,
            @Autowired(required = false) DeviceRepository deviceRepository,
            @Autowired(required = false) ConfigurationRepository configurationRepository) {
        this.normalizedConfigRepository = normalizedConfigRepository;
        this.parserService = parserService;
        this.complianceRuleRepository = complianceRuleRepository;
        this.controlRepository = controlRepository;
        this.ruleApplicabilityChecker = ruleApplicabilityChecker;
        this.ruleEvaluator = ruleEvaluator;
        this.riskCalculationService = riskCalculationService;
        this.findingRepository = findingRepository;
        this.simulationRepository = simulationRepository;
        this.deviceRepository = deviceRepository;
        this.configurationRepository = configurationRepository;
    }

    public WhatIfSimulationServiceImpl(
            NormalizedConfigurationRepository normalizedConfigRepository,
            ParserService parserService,
            ComplianceRuleRepository complianceRuleRepository,
            ControlRepository controlRepository,
            RuleApplicabilityChecker ruleApplicabilityChecker,
            RuleEvaluator ruleEvaluator,
            RiskCalculationService riskCalculationService,
            FindingRepository findingRepository,
            WhatIfSimulationRepository simulationRepository) {
        this(normalizedConfigRepository, parserService, complianceRuleRepository, controlRepository,
                ruleApplicabilityChecker, ruleEvaluator, riskCalculationService, findingRepository,
                simulationRepository, null, null);
    }

    @Override
    public WhatIfSimulationResult simulate(WhatIfSimulationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("What-If simulation request must not be null");
        }
        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            throw new IllegalArgumentException("What-If simulation requires non-blank deviceId");
        }
        if (request.getBaseConfigurationVersionId() == null || request.getBaseConfigurationVersionId().isBlank()) {
            request.setBaseConfigurationVersionId("v1");
        }

        // 0. Determine device details dynamically
        Device device = (deviceRepository != null)
                ? deviceRepository.findById(request.getDeviceId().trim()).orElse(null)
                : null;
        String vendor = device != null && device.getVendor() != null ? device.getVendor() : "Cisco";
        String platform = device != null && device.getPlatform() != null ? device.getPlatform() : "IOS-XE";
        String osVersion = device != null && device.getOsVersion() != null ? device.getOsVersion() : "17.3";

        // 1. Retrieve base configuration safely
        NormalizedConfigurationDocument baseDoc = findBaseConfiguration(
                request.getDeviceId().trim(), request.getBaseConfigurationVersionId().trim()
        );

        CanonicalSecurityModel baseCanonical = null;

        if (baseDoc != null && baseDoc.getCanonical() != null) {
            baseCanonical = cloneCanonical(baseDoc.getCanonical());
            if (baseDoc.getVendor() != null) vendor = baseDoc.getVendor();
            if (baseDoc.getPlatform() != null) platform = baseDoc.getPlatform();
            if (baseDoc.getOsVersion() != null) osVersion = baseDoc.getOsVersion();
        }

        // If pre-normalized model is not in database, retrieve raw configuration and parse on-demand
        if (baseCanonical == null || (baseCanonical.getSecurity().isEmpty() && baseCanonical.getManagementAccess().isEmpty() && baseCanonical.getAuthentication().isEmpty())) {
            Configuration rawConfig = null;
            if (configurationRepository != null) {
                if (request.getBaseConfigurationVersionId() != null && !request.getBaseConfigurationVersionId().isBlank()) {
                    rawConfig = configurationRepository.findById(request.getBaseConfigurationVersionId().trim()).orElse(null);
                }
                if (rawConfig == null) {
                    List<Configuration> configs = configurationRepository.findByDeviceId(request.getDeviceId().trim());
                    if (configs != null && !configs.isEmpty()) {
                        rawConfig = configs.get(0);
                    }
                }
            }

            if (rawConfig != null && rawConfig.getLines() != null && !rawConfig.getLines().isEmpty()) {
                String baseRaw = String.join("\n", rawConfig.getLines());
                ParserResult baseParse = parserService.parse(baseRaw, vendor, platform, true);
                if (baseParse != null && baseParse.getCanonical() != null) {
                    baseCanonical = cloneCanonical(baseParse.getCanonical());
                }
            }
        }

        if (baseCanonical == null) {
            baseCanonical = new CanonicalSecurityModel();
        }

        CanonicalSecurityModel simulatedCanonical = cloneCanonical(baseCanonical);

        // 2. Build simulated canonical model by merging proposed changes onto base configuration
        if (request.getProposedRawConfig() != null && !request.getProposedRawConfig().isBlank()) {
            ParserResult parserResult = parserService.parse(
                    request.getProposedRawConfig(),
                    vendor,
                    platform,
                    true
            );
            if (parserResult != null && parserResult.getCanonical() != null) {
                mergeCanonical(simulatedCanonical, parserResult.getCanonical());
            }
        }

        // Apply explicit canonical overrides if specified
        if (request.getCanonicalOverrides() != null && !request.getCanonicalOverrides().isEmpty()) {
            for (Map.Entry<String, Object> entry : request.getCanonicalOverrides().entrySet()) {
                CanonicalFieldAllowlist.applyToCanonical(simulatedCanonical, entry.getKey(), entry.getValue());
            }
        }

        // 3. Load active compliance rules
        List<ComplianceRuleDocument> activeRuleDocs = complianceRuleRepository.findAll().stream()
                .filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus()))
                .toList();

        // 4. Evaluate rules before
        Map<String, RuleResultStatus> beforeStatuses = new LinkedHashMap<>();
        int beforePassed = 0;
        int beforeFailed = 0;
        int beforeApplicable = 0;
        Map<String, Integer> fieldRiskMapBefore = new LinkedHashMap<>();

        for (ComplianceRuleDocument ruleDoc : activeRuleDocs) {
            ComplianceRule domainRule = toDomainRule(ruleDoc);
            if (!ruleApplicabilityChecker.isApplicable(domainRule, vendor, platform, osVersion)) {
                continue;
            }
            beforeApplicable++;
            RuleEvaluationResult eval = ruleEvaluator.evaluate(baseCanonical, domainRule);
            RuleResultStatus status = eval != null ? eval.getStatus() : RuleResultStatus.ERROR;
            String ruleKey = ruleDoc.getRuleCode() != null ? ruleDoc.getRuleCode() : ruleDoc.getId();
            beforeStatuses.put(ruleKey, status);
            if (status == RuleResultStatus.PASS) {
                beforePassed++;
            } else if (status == RuleResultStatus.FAIL) {
                beforeFailed++;
                String fieldPath = ruleDoc.getExpression() != null && ruleDoc.getExpression().getField() != null
                        ? ruleDoc.getExpression().getField() : ruleDoc.getId();
                int r = computeSimulatedRisk(ruleDoc, request.getDeviceId());
                fieldRiskMapBefore.merge(fieldPath, r, Math::max);
            }
        }

        // 5. Evaluate rules after
        Map<String, RuleResultStatus> afterStatuses = new LinkedHashMap<>();
        int afterPassed = 0;
        int afterFailed = 0;
        int afterApplicable = 0;
        Map<String, Integer> fieldRiskMapAfter = new LinkedHashMap<>();
        List<String> affectedControlIds = new ArrayList<>();

        for (ComplianceRuleDocument ruleDoc : activeRuleDocs) {
            ComplianceRule domainRule = toDomainRule(ruleDoc);
            if (!ruleApplicabilityChecker.isApplicable(domainRule, vendor, platform, osVersion)) {
                continue;
            }
            afterApplicable++;
            RuleEvaluationResult eval = ruleEvaluator.evaluate(simulatedCanonical, domainRule);
            RuleResultStatus status = eval != null ? eval.getStatus() : RuleResultStatus.ERROR;
            String ruleKey = ruleDoc.getRuleCode() != null ? ruleDoc.getRuleCode() : ruleDoc.getId();
            afterStatuses.put(ruleKey, status);
            if (status == RuleResultStatus.PASS) {
                afterPassed++;
            } else if (status == RuleResultStatus.FAIL) {
                afterFailed++;
                String fieldPath = ruleDoc.getExpression() != null && ruleDoc.getExpression().getField() != null
                        ? ruleDoc.getExpression().getField() : ruleDoc.getId();
                int r = computeSimulatedRisk(ruleDoc, request.getDeviceId());
                fieldRiskMapAfter.merge(fieldPath, r, Math::max);
            }

            RuleResultStatus bStatus = beforeStatuses.get(ruleKey);
            if (bStatus != null && bStatus != status) {
                if (ruleDoc.getControlId() != null && !affectedControlIds.contains(ruleDoc.getControlId())) {
                    affectedControlIds.add(ruleDoc.getControlId());
                }
            }
        }

        int totalRiskBefore = fieldRiskMapBefore.values().stream().mapToInt(Integer::intValue).sum();
        totalRiskBefore = Math.min(100, Math.max(0, totalRiskBefore));

        int totalRiskAfter = fieldRiskMapAfter.values().stream().mapToInt(Integer::intValue).sum();
        totalRiskAfter = Math.min(100, Math.max(0, totalRiskAfter));

        int totalEvaluatedBefore = beforePassed + beforeFailed;
        Double beforeCompliance = (totalEvaluatedBefore > 0)
                ? Math.round(((double) beforePassed / (double) totalEvaluatedBefore) * 1000.0) / 10.0
                : (beforeApplicable > 0 ? 0.0 : null);

        int totalEvaluatedAfter = afterPassed + afterFailed;
        Double afterCompliance = (totalEvaluatedAfter > 0)
                ? Math.round(((double) afterPassed / (double) totalEvaluatedAfter) * 1000.0) / 10.0
                : (afterApplicable > 0 ? 0.0 : null);

        // 6. Detect changes and classify them
        List<SimulationChange> changes = detectChanges(baseCanonical, simulatedCanonical, activeRuleDocs);

        // 7. Calculate impact using drift impact precedence
        String impact = determineImpact(changes, totalRiskBefore, totalRiskAfter);

        // 8. Resolve affected findings if available
        List<String> affectedFindingIds = new ArrayList<>();
        if (findingRepository != null) {
            List<FindingDocument> existingFindings = findingRepository.findByDeviceId(request.getDeviceId());
            for (FindingDocument f : existingFindings) {
                if (f.getCanonicalField() != null) {
                    for (SimulationChange c : changes) {
                        if (isFieldMatching(c.getCanonicalField(), f.getCanonicalField())) {
                            affectedFindingIds.add(f.getId());
                            break;
                        }
                    }
                }
            }
        }

        // 9. Construct result
        WhatIfSimulationResult result = new WhatIfSimulationResult();
        result.setDeviceId(request.getDeviceId());
        result.setBaseConfigurationVersionId(request.getBaseConfigurationVersionId());
        result.setStatus("COMPLETED");

        result.setBefore(new SimulationPosture(beforeCompliance, totalRiskBefore, beforeFailed, beforePassed));
        result.setAfter(new SimulationPosture(afterCompliance, totalRiskAfter, afterFailed, afterPassed));

        result.setFindingDelta(afterFailed - beforeFailed);
        result.setRiskDelta(totalRiskAfter - totalRiskBefore);
        result.setImpact(impact);

        result.setChanges(changes);
        result.setAffectedControlIds(affectedControlIds);
        result.setAffectedFindingIds(affectedFindingIds);

        Map<String, String> bMap = new LinkedHashMap<>();
        beforeStatuses.forEach((k, v) -> bMap.put(k, v.name()));
        result.setRuleResultsBefore(bMap);

        Map<String, String> aMap = new LinkedHashMap<>();
        afterStatuses.forEach((k, v) -> aMap.put(k, v.name()));
        result.setRuleResultsAfter(aMap);

        // 10. Optional persistence to what_if_simulations only if requested
        if (request.isPersist() && simulationRepository != null) {
            WhatIfSimulationDocument doc = new WhatIfSimulationDocument();
            doc.setId(UUID.randomUUID().toString());
            doc.setDeviceId(request.getDeviceId());
            doc.setBaseConfigurationVersionId(request.getBaseConfigurationVersionId());
            doc.setName(request.getName());
            doc.setDescription(request.getDescription());
            doc.setChanges(changes);
            doc.setStatus("COMPLETED");
            doc.setBefore(result.getBefore());
            doc.setAfter(result.getAfter());
            doc.setAffectedControlIds(affectedControlIds);
            doc.setAffectedFindingIds(affectedFindingIds);
            doc.setCreatedBy(request.getCreatedBy());
            Instant now = Instant.now();
            doc.setCreatedAt(now);
            doc.setUpdatedAt(now);

            WhatIfSimulationDocument saved = simulationRepository.save(doc);
            result.setPersistedSimulationId(saved.getId());
        }

        return result;
    }

    private void mergeCanonical(CanonicalSecurityModel target, CanonicalSecurityModel overlay) {
        if (overlay == null || target == null) return;
        mergeMap(target.getSecurity(), overlay.getSecurity());
        mergeMap(target.getAuthentication(), overlay.getAuthentication());
        mergeMap(target.getLogging(), overlay.getLogging());
        mergeMap(target.getNtp(), overlay.getNtp());
        mergeMap(target.getManagementAccess(), overlay.getManagementAccess());
        mergeMap(target.getAcl(), overlay.getAcl());
        mergeMap(target.getCrypto(), overlay.getCrypto());
        mergeMap(target.getServices(), overlay.getServices());
    }

    @SuppressWarnings("unchecked")
    private void mergeMap(Map<String, Object> target, Map<String, Object> overlay) {
        if (overlay == null || target == null) return;
        for (Map.Entry<String, Object> entry : overlay.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> overlayNested && target.get(entry.getKey()) instanceof Map<?, ?> targetNested) {
                mergeMap((Map<String, Object>) targetNested, (Map<String, Object>) overlayNested);
            } else if (entry.getValue() != null) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private NormalizedConfigurationDocument findBaseConfiguration(String deviceId, String versionId) {
        List<NormalizedConfigurationDocument> docs = normalizedConfigRepository.findByDeviceId(deviceId);
        if (docs == null || docs.isEmpty()) return null;
        return docs.stream()
                .filter(d -> versionId.equalsIgnoreCase(d.getVersionId()) || versionId.equalsIgnoreCase(d.getId()) || versionId.equalsIgnoreCase(d.getConfigurationId()))
                .findFirst()
                .orElse(docs.get(0));
    }

    private int computeSimulatedRisk(ComplianceRuleDocument ruleDoc, String deviceId) {
        if (riskCalculationService == null) {
            return 50;
        }
        Finding finding = new Finding();
        finding.setId("sim-finding-" + UUID.randomUUID());
        finding.setDeviceId(deviceId);
        finding.setSeverity(ruleDoc.getSeverity());
        finding.setCanonicalField(ruleDoc.getExpression() != null ? ruleDoc.getExpression().getField() : null);

        ComplianceRule rule = toDomainRule(ruleDoc);
        RiskAssessment ra = riskCalculationService.calculateRisk(finding, rule, new RiskContext(1.0));
        return ra != null ? ra.getScore() : 50;
    }

    private List<SimulationChange> detectChanges(
            CanonicalSecurityModel before, CanonicalSecurityModel after, List<ComplianceRuleDocument> activeRules) {
        List<SimulationChange> changes = new ArrayList<>();
        Set<String> fields = CanonicalFieldAllowlist.getAllowedFields();

        for (String field : fields) {
            Object oldVal = CanonicalFieldAllowlist.extractFromCanonical(before, field);
            Object newVal = CanonicalFieldAllowlist.extractFromCanonical(after, field);

            if (!Objects.equals(oldVal, newVal)) {
                String classification = classifyChange(field, oldVal, newVal, activeRules);
                changes.add(new SimulationChange(field, oldVal, newVal, classification));
            }
        }
        return changes;
    }

    private boolean isFieldMatching(String canonicalPath, String ruleField) {
        if (canonicalPath == null || ruleField == null) return false;
        if (canonicalPath.equalsIgnoreCase(ruleField)) return true;
        String cNorm = canonicalPath.replaceAll("^(security\\.|management\\.|system\\.|monitoring\\.)", "");
        String rNorm = ruleField.replaceAll("^(security\\.|management\\.|system\\.|monitoring\\.)", "");
        return cNorm.equalsIgnoreCase(rNorm);
    }

    private String classifyChange(String field, Object oldVal, Object newVal, List<ComplianceRuleDocument> activeRules) {
        List<ComplianceRuleDocument> fieldRules = activeRules.stream()
                .filter(r -> r.getExpression() != null && isFieldMatching(field, r.getExpression().getField()))
                .toList();

        if (fieldRules.isEmpty()) {
            return DriftClassification.UNKNOWN_IMPACT.name();
        }

        boolean hasImproved = false;
        boolean hasDegraded = false;

        for (ComplianceRuleDocument r : fieldRules) {
            RuleResultStatus beforeStatus = evaluatePure(r, oldVal);
            RuleResultStatus afterStatus = evaluatePure(r, newVal);

            if (beforeStatus == RuleResultStatus.FAIL && afterStatus == RuleResultStatus.PASS) {
                hasImproved = true;
            } else if (beforeStatus == RuleResultStatus.PASS && afterStatus == RuleResultStatus.FAIL) {
                hasDegraded = true;
            }
        }

        if (hasImproved && !hasDegraded) {
            return DriftClassification.IMPROVED.name();
        } else if (hasDegraded && !hasImproved) {
            return DriftClassification.DEGRADED.name();
        } else if (hasImproved && hasDegraded) {
            return "MIXED";
        } else {
            return DriftClassification.NO_SECURITY_IMPACT.name();
        }
    }

    private RuleResultStatus evaluatePure(ComplianceRuleDocument ruleDoc, Object val) {
        if (val == null) {
            return RuleResultStatus.UNKNOWN;
        }
        if (ruleDoc.getExpression() == null) {
            return RuleResultStatus.UNKNOWN;
        }
        String op = ruleDoc.getExpression().getOperator();
        Object expected = ruleDoc.getExpression().getValue();

        if ("EQUALS".equalsIgnoreCase(op)) {
            return Objects.equals(String.valueOf(val).trim(), String.valueOf(expected).trim())
                    ? RuleResultStatus.PASS : RuleResultStatus.FAIL;
        }
        return RuleResultStatus.UNKNOWN;
    }

    private String determineImpact(List<SimulationChange> changes, int totalRiskBefore, int totalRiskAfter) {
        if (changes.isEmpty()) {
            return "NO_CHANGE";
        }
        boolean hasUnknown = changes.stream().anyMatch(c -> DriftClassification.UNKNOWN_IMPACT.name().equals(c.getClassification()));
        if (hasUnknown) {
            return "UNKNOWN";
        }
        if (totalRiskAfter > totalRiskBefore) {
            return "INCREASED";
        }
        if (totalRiskAfter < totalRiskBefore) {
            return "DECREASED";
        }

        boolean hasImproved = changes.stream().anyMatch(c -> DriftClassification.IMPROVED.name().equals(c.getClassification()));
        boolean hasDegraded = changes.stream().anyMatch(c -> DriftClassification.DEGRADED.name().equals(c.getClassification()));

        if (hasImproved && hasDegraded) {
            return "MIXED";
        } else if (hasDegraded) {
            return "INCREASED";
        } else if (hasImproved) {
            return "DECREASED";
        } else {
            return "NO_CHANGE";
        }
    }

    private CanonicalSecurityModel cloneCanonical(CanonicalSecurityModel src) {
        if (src == null) return new CanonicalSecurityModel();
        CanonicalSecurityModel copy = new CanonicalSecurityModel();
        copy.setSecurity(deepCopyMap(src.getSecurity()));
        copy.setAuthentication(deepCopyMap(src.getAuthentication()));
        copy.setLogging(deepCopyMap(src.getLogging()));
        copy.setNtp(deepCopyMap(src.getNtp()));
        copy.setManagementAccess(deepCopyMap(src.getManagementAccess()));
        copy.setAcl(deepCopyMap(src.getAcl()));
        copy.setCrypto(deepCopyMap(src.getCrypto()));
        copy.setServices(deepCopyMap(src.getServices()));
        return copy;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopyMap(Map<String, Object> map) {
        if (map == null) return new LinkedHashMap<>();
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getValue() instanceof Map<?, ?> m) {
                copy.put(e.getKey(), deepCopyMap((Map<String, Object>) m));
            } else {
                copy.put(e.getKey(), e.getValue());
            }
        }
        return copy;
    }

    private ComplianceRule toDomainRule(ComplianceRuleDocument doc) {
        ComplianceRule rule = new ComplianceRule();
        rule.setId(doc.getId());
        rule.setRuleCode(doc.getRuleCode() != null ? doc.getRuleCode() : doc.getId());
        rule.setControlId(doc.getControlId());
        rule.setName(doc.getName());
        rule.setDescription(doc.getDescription());
        rule.setRequirement(doc.getExpression());
        rule.setSeverity(doc.getSeverity());
        rule.setFrameworkIds(doc.getFrameworkIds());
        rule.setBaselines(doc.getBaselines());
        rule.setApplicableVendors(doc.getApplicableVendors());
        rule.setApplicablePlatforms(doc.getApplicablePlatforms());
        rule.setApplicableOsVersions(doc.getApplicableOsVersions());
        rule.setStatus(doc.getStatus());
        rule.setVersion(doc.getVersion());
        return rule;
    }
}
