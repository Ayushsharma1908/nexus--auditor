package com.nexuscomply.cyber.drift.service;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.compliance.GenericRuleEvaluator;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.compliance.RuleRequirement;
import com.nexuscomply.cyber.compliance.RuleResultStatus;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleDocument;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleRepository;
import com.nexuscomply.cyber.drift.model.DriftChange;
import com.nexuscomply.cyber.drift.model.DriftClassification;
import com.nexuscomply.cyber.drift.model.DriftEvent;
import com.nexuscomply.cyber.drift.persistence.DriftEventDocument;
import com.nexuscomply.cyber.drift.persistence.DriftEventRepository;
import com.nexuscomply.cyber.finding.Finding;
import com.nexuscomply.cyber.finding.persistence.FindingDocument;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import com.nexuscomply.cyber.risk.RiskAssessment;
import com.nexuscomply.cyber.risk.RiskCalculationService;
import com.nexuscomply.cyber.risk.RiskContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DriftDetectionServiceImpl implements DriftDetectionService {

    private final DriftEventRepository driftEventRepository;
    private final NormalizedConfigurationRepository normalizedConfigRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final FindingRepository findingRepository;
    private final RiskCalculationService riskCalculationService;
    private final GenericRuleEvaluator ruleEvaluator;

    public DriftDetectionServiceImpl(
            DriftEventRepository driftEventRepository,
            NormalizedConfigurationRepository normalizedConfigRepository,
            ComplianceRuleRepository ruleRepository,
            FindingRepository findingRepository,
            RiskCalculationService riskCalculationService) {
        this(driftEventRepository, normalizedConfigRepository, ruleRepository, findingRepository, riskCalculationService, new GenericRuleEvaluator());
    }

    public DriftDetectionServiceImpl(
            DriftEventRepository driftEventRepository,
            NormalizedConfigurationRepository normalizedConfigRepository,
            ComplianceRuleRepository ruleRepository,
            FindingRepository findingRepository,
            RiskCalculationService riskCalculationService,
            GenericRuleEvaluator ruleEvaluator) {
        this.driftEventRepository = driftEventRepository;
        this.normalizedConfigRepository = normalizedConfigRepository;
        this.ruleRepository = ruleRepository;
        this.findingRepository = findingRepository;
        this.riskCalculationService = riskCalculationService;
        this.ruleEvaluator = ruleEvaluator != null ? ruleEvaluator : new GenericRuleEvaluator();
    }

    @Override
    public DriftEvent detectDrift(NormalizedConfigurationDocument beforeDoc, NormalizedConfigurationDocument afterDoc) {
        // Absolute Rule 1: Drift compares TWO NormalizedConfigurationDocuments for the SAME deviceId
        if (beforeDoc == null || afterDoc == null) {
            throw new IllegalArgumentException("Both beforeDoc and afterDoc must be non-null for drift detection");
        }
        if (beforeDoc.getDeviceId() == null || afterDoc.getDeviceId() == null) {
            throw new IllegalArgumentException("Both configuration documents must have a valid deviceId");
        }
        if (!beforeDoc.getDeviceId().equals(afterDoc.getDeviceId())) {
            throw new IllegalArgumentException("Drift comparison must be for the same deviceId. Received: "
                    + beforeDoc.getDeviceId() + " vs " + afterDoc.getDeviceId());
        }
        String deviceId = beforeDoc.getDeviceId();
        String fromVersionId = beforeDoc.getVersionId();
        String toVersionId = afterDoc.getVersionId();

        if (beforeDoc.getId() != null && beforeDoc.getId().equals(afterDoc.getId())) {
            throw new IllegalArgumentException("Cannot compare identical configuration document: " + beforeDoc.getId());
        }
        if (fromVersionId != null && fromVersionId.equals(toVersionId)) {
            throw new IllegalArgumentException("Cannot compare identical versionId: " + fromVersionId);
        }

        // Absolute Rule 2: Only security-relevant canonical facts count. Filter out cosmetic noise (unknowns).
        Map<String, Object> beforeMap = flattenCanonical(beforeDoc.getCanonical());
        Map<String, Object> afterMap = flattenCanonical(afterDoc.getCanonical());

        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(beforeMap.keySet());
        allKeys.addAll(afterMap.keySet());

        List<ComplianceRuleDocument> allRules = ruleRepository.findAll();
        String vendor = afterDoc.getVendor() != null ? afterDoc.getVendor() : beforeDoc.getVendor();
        String platform = afterDoc.getPlatform() != null ? afterDoc.getPlatform() : beforeDoc.getPlatform();

        List<DriftChange> changes = new ArrayList<>();
        Set<String> affectedControlIds = new LinkedHashSet<>();
        Set<String> changedFieldPaths = new LinkedHashSet<>();

        CanonicalSecurityModel canonBefore = beforeDoc.getCanonical() != null ? beforeDoc.getCanonical() : new CanonicalSecurityModel();
        CanonicalSecurityModel canonAfter = afterDoc.getCanonical() != null ? afterDoc.getCanonical() : new CanonicalSecurityModel();

        int totalRiskBefore = 0;
        int totalRiskAfter = 0;

        for (String fieldPath : allKeys) {
            Object valBefore = beforeMap.get(fieldPath);
            Object valAfter = afterMap.get(fieldPath);

            if (areEqual(valBefore, valAfter)) {
                continue; // No canonical difference
            }

            changedFieldPaths.add(fieldPath);

            String changeType;
            if (valBefore != null && valAfter != null) {
                changeType = "MODIFIED";
            } else if (valBefore == null && valAfter != null) {
                changeType = "ADDED";
            } else {
                changeType = "REMOVED";
            }

            String sourceBefore = extractSource(beforeDoc.getSourceMap(), fieldPath);
            String sourceAfter = extractSource(afterDoc.getSourceMap(), fieldPath);

            // Absolute Rule 3: determine affected controls via ComplianceRule matching RuleRequirement.canonicalField
            List<ComplianceRuleDocument> applicableRules = findApplicableRules(allRules, fieldPath, vendor, platform);

            // Absolute Rule 5: Classify each change as IMPROVED, DEGRADED, or NO_SECURITY_IMPACT
            // If no rule exists, classify as UNKNOWN_IMPACT rather than guessing.
            String classification;
            int fieldRiskBefore = 0;
            int fieldRiskAfter = 0;

            if (applicableRules.isEmpty()) {
                classification = DriftClassification.UNKNOWN_IMPACT.name();
            } else {
                List<String> ruleClassifications = new ArrayList<>();

                for (ComplianceRuleDocument ruleDoc : applicableRules) {
                    if (ruleDoc.getControlId() != null) {
                        affectedControlIds.add(ruleDoc.getControlId());
                    }

                    ComplianceRule rule = toDomainRule(ruleDoc);
                    RuleEvaluationResult resBefore = ruleEvaluator.evaluate(canonBefore, rule);
                    RuleEvaluationResult resAfter = ruleEvaluator.evaluate(canonAfter, rule);

                    RuleResultStatus sBefore = resBefore.getStatus();
                    RuleResultStatus sAfter = resAfter.getStatus();

                    // Absolute Rule 4: Compute risk delta reusing RiskCalculationService on before & after states
                    // Risk per field = max over matching rules
                    if (sBefore == RuleResultStatus.FAIL) {
                        int rBefore = computeRiskForState(deviceId, beforeDoc.getConfigurationId(), fieldPath, valBefore, ruleDoc);
                        fieldRiskBefore = Math.max(fieldRiskBefore, rBefore);
                    }
                    if (sAfter == RuleResultStatus.FAIL) {
                        int rAfter = computeRiskForState(deviceId, afterDoc.getConfigurationId(), fieldPath, valAfter, ruleDoc);
                        fieldRiskAfter = Math.max(fieldRiskAfter, rAfter);
                    }

                    // Rule transition classification:
                    // FAIL->PASS IMPROVED; PASS->FAIL DEGRADED; UNKNOWN->FAIL DEGRADED;
                    // UNKNOWN->PASS NO_SECURITY_IMPACT; FAIL->UNKNOWN UNKNOWN_IMPACT;
                    // PASS->UNKNOWN UNKNOWN_IMPACT; no applicable rule UNKNOWN_IMPACT
                    String ruleClass;
                    if (sBefore == RuleResultStatus.FAIL && sAfter == RuleResultStatus.PASS) {
                        ruleClass = DriftClassification.IMPROVED.name();
                    } else if (sBefore == RuleResultStatus.PASS && sAfter == RuleResultStatus.FAIL) {
                        ruleClass = DriftClassification.DEGRADED.name();
                    } else if (sBefore == RuleResultStatus.UNKNOWN && sAfter == RuleResultStatus.FAIL) {
                        ruleClass = DriftClassification.DEGRADED.name();
                    } else if (sBefore == RuleResultStatus.UNKNOWN && sAfter == RuleResultStatus.PASS) {
                        ruleClass = DriftClassification.NO_SECURITY_IMPACT.name();
                    } else if (sBefore == RuleResultStatus.FAIL && sAfter == RuleResultStatus.UNKNOWN) {
                        ruleClass = DriftClassification.UNKNOWN_IMPACT.name();
                    } else if (sBefore == RuleResultStatus.PASS && sAfter == RuleResultStatus.UNKNOWN) {
                        ruleClass = DriftClassification.UNKNOWN_IMPACT.name();
                    } else {
                        ruleClass = DriftClassification.NO_SECURITY_IMPACT.name();
                    }
                    ruleClassifications.add(ruleClass);
                }

                if (ruleClassifications.contains(DriftClassification.DEGRADED.name())) {
                    classification = DriftClassification.DEGRADED.name();
                } else if (ruleClassifications.contains(DriftClassification.IMPROVED.name())) {
                    classification = DriftClassification.IMPROVED.name();
                } else if (ruleClassifications.contains(DriftClassification.UNKNOWN_IMPACT.name())) {
                    classification = DriftClassification.UNKNOWN_IMPACT.name();
                } else {
                    classification = DriftClassification.NO_SECURITY_IMPACT.name();
                }
            }

            totalRiskBefore += fieldRiskBefore;
            totalRiskAfter += fieldRiskAfter;

            changes.add(new DriftChange(fieldPath, valBefore, valAfter, changeType, sourceBefore, sourceAfter, classification));
        }

        // Clamp event-level riskBefore and riskAfter to 0-100
        totalRiskBefore = Math.min(100, Math.max(0, totalRiskBefore));
        totalRiskAfter = Math.min(100, Math.max(0, totalRiskAfter));

        // Link existing findings for this device matching changed fields on the before configuration
        List<String> affectedFindingIds = resolveAffectedFindings(deviceId, beforeDoc.getConfigurationId(), changedFieldPaths);

        // Overall Impact determination:
        // risk up INCREASED; down DECREASED; equal with any UNKNOWN_IMPACT UNKNOWN;
        // equal with both IMPROVED and DEGRADED MIXED; zero changes NO_CHANGE.
        String impact;
        if (changes.isEmpty()) {
            impact = "NO_CHANGE";
        } else if (totalRiskAfter > totalRiskBefore) {
            impact = "INCREASED";
        } else if (totalRiskAfter < totalRiskBefore) {
            impact = "DECREASED";
        } else {
            // totalRiskAfter == totalRiskBefore
            boolean hasUnknown = changes.stream().anyMatch(c -> DriftClassification.UNKNOWN_IMPACT.name().equals(c.getClassification()));
            boolean hasImproved = changes.stream().anyMatch(c -> DriftClassification.IMPROVED.name().equals(c.getClassification()));
            boolean hasDegraded = changes.stream().anyMatch(c -> DriftClassification.DEGRADED.name().equals(c.getClassification()));

            if (hasUnknown) {
                impact = "UNKNOWN";
            } else if (hasImproved && hasDegraded) {
                impact = "MIXED";
            } else if (hasDegraded) {
                impact = "INCREASED";
            } else if (hasImproved) {
                impact = "DECREASED";
            } else {
                impact = "NO_CHANGE";
            }
        }

        Instant now = Instant.now();
        DriftEventDocument doc = new DriftEventDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setDeviceId(deviceId);
        doc.setFromVersionId(fromVersionId);
        doc.setToVersionId(toVersionId);
        doc.setFromVersion(extractVersionNumber(fromVersionId, beforeDoc.getOsVersion()));
        doc.setToVersion(extractVersionNumber(toVersionId, afterDoc.getOsVersion()));
        doc.setChanges(changes);
        doc.setAffectedControlIds(new ArrayList<>(affectedControlIds));
        doc.setAffectedFindingIds(affectedFindingIds);
        doc.setRiskBefore(totalRiskBefore);
        doc.setRiskAfter(totalRiskAfter);
        doc.setImpact(impact);
        doc.setDetectedAt(now);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);

        DriftEventDocument saved = driftEventRepository.save(doc);
        return toDomain(saved);
    }

    @Override
    public DriftEvent detectDriftByVersionIds(String deviceId, String fromVersionId, String toVersionId) {
        if (fromVersionId != null && fromVersionId.equals(toVersionId)) {
            throw new IllegalArgumentException("Cannot compare identical versionId: " + fromVersionId);
        }
        NormalizedConfigurationDocument beforeDoc = normalizedConfigRepository.findByVersionId(fromVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Normalized configuration not found for fromVersionId: " + fromVersionId));
        NormalizedConfigurationDocument afterDoc = normalizedConfigRepository.findByVersionId(toVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Normalized configuration not found for toVersionId: " + toVersionId));
        return detectDrift(beforeDoc, afterDoc);
    }

    @Override
    public DriftEvent detectDriftByDocumentIds(String fromDocId, String toDocId) {
        if (fromDocId != null && fromDocId.equals(toDocId)) {
            throw new IllegalArgumentException("Cannot compare identical document id: " + fromDocId);
        }
        NormalizedConfigurationDocument beforeDoc = normalizedConfigRepository.findById(fromDocId)
                .orElseThrow(() -> new IllegalArgumentException("Normalized configuration not found for id: " + fromDocId));
        NormalizedConfigurationDocument afterDoc = normalizedConfigRepository.findById(toDocId)
                .orElseThrow(() -> new IllegalArgumentException("Normalized configuration not found for id: " + toDocId));
        return detectDrift(beforeDoc, afterDoc);
    }

    @Override
    public Optional<DriftEvent> getDriftEventById(String id) {
        return driftEventRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<DriftEvent> getDriftEventsByDeviceId(String deviceId) {
        return driftEventRepository.findByDeviceIdOrderByDetectedAtDesc(deviceId).stream()
                .map(this::toDomain)
                .toList();
    }

    private int computeRiskForState(String deviceId, String configId, String fieldPath, Object val, ComplianceRuleDocument ruleDoc) {
        Finding finding = new Finding();
        finding.setId("drift-eval-" + UUID.randomUUID());
        finding.setDeviceId(deviceId);
        finding.setConfigurationId(configId);
        finding.setControlId(ruleDoc.getControlId());
        finding.setRuleId(ruleDoc.getId());
        finding.setControlCode(ruleDoc.getRuleCode());
        finding.setCanonicalField(fieldPath);
        finding.setSeverity(ruleDoc.getSeverity());
        finding.setExpected(ruleDoc.getExpression() != null ? ruleDoc.getExpression().getExpectedValue() : null);
        finding.setActual(val);

        ComplianceRule rule = toDomainRule(ruleDoc);

        RiskContext context = new RiskContext(1.0);
        RiskAssessment assessment = riskCalculationService.calculateRisk(finding, rule, context);
        return (int) Math.round(assessment.getScore());
    }

    private ComplianceRule toDomainRule(ComplianceRuleDocument doc) {
        ComplianceRule rule = new ComplianceRule();
        rule.setId(doc.getId());
        rule.setControlId(doc.getControlId());
        rule.setRuleCode(doc.getRuleCode());
        rule.setName(doc.getName());
        rule.setDescription(doc.getDescription());
        rule.setRequirement(doc.getExpression());
        rule.setSeverity(doc.getSeverity());
        rule.setFrameworkIds(doc.getFrameworkIds());
        rule.setApplicableVendors(doc.getApplicableVendors());
        rule.setApplicablePlatforms(doc.getApplicablePlatforms());
        rule.setApplicableOsVersions(doc.getApplicableOsVersions());
        return rule;
    }

    private List<ComplianceRuleDocument> findApplicableRules(
            List<ComplianceRuleDocument> allRules,
            String canonicalField,
            String vendor,
            String platform) {
        List<ComplianceRuleDocument> matched = new ArrayList<>();
        for (ComplianceRuleDocument r : allRules) {
            if (r.getExpression() == null || !canonicalField.equals(r.getExpression().getCanonicalField())) {
                continue;
            }
            if (vendor != null && r.getApplicableVendors() != null && !r.getApplicableVendors().isEmpty()) {
                boolean vendorMatch = r.getApplicableVendors().stream().anyMatch(v -> v.equalsIgnoreCase(vendor));
                if (!vendorMatch) {
                    continue;
                }
            }
            if (platform != null && r.getApplicablePlatforms() != null && !r.getApplicablePlatforms().isEmpty()) {
                boolean platMatch = r.getApplicablePlatforms().stream().anyMatch(p -> p.equalsIgnoreCase(platform));
                if (!platMatch) {
                    continue;
                }
            }
            matched.add(r);
        }
        return matched;
    }

    private String extractSource(List<SourceMapEntry> sourceMap, String canonicalField) {
        if (sourceMap == null || sourceMap.isEmpty()) return null;
        return sourceMap.stream()
                .filter(sm -> canonicalField.equals(sm.getCanonicalField()))
                .map(SourceMapEntry::getRawText)
                .findFirst()
                .orElse(null);
    }

    private List<String> resolveAffectedFindings(String deviceId, String beforeConfigId, Set<String> changedFields) {
        if (deviceId == null || changedFields == null || changedFields.isEmpty()) {
            return Collections.emptyList();
        }
        List<FindingDocument> deviceFindings = findingRepository.findByDeviceId(deviceId);
        if (deviceFindings == null || deviceFindings.isEmpty()) {
            return Collections.emptyList();
        }
        return deviceFindings.stream()
                .filter(f -> beforeConfigId == null || beforeConfigId.equals(f.getConfigurationId()))
                .filter(f -> changedFields.contains(f.getCanonicalField()))
                .map(FindingDocument::getId)
                .distinct()
                .toList();
    }

    private Object extractVersionNumber(String versionId, String osVersion) {
        if (versionId != null) {
            Matcher m = Pattern.compile("\\d+").matcher(versionId);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group());
                } catch (NumberFormatException ignored) {}
            }
        }
        return osVersion != null ? osVersion : versionId;
    }

    private Map<String, Object> flattenCanonical(CanonicalSecurityModel canonical) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (canonical == null) return map;

        flattenDomain(map, "security", canonical.getSecurity());
        flattenDomain(map, "authentication", canonical.getAuthentication());
        flattenDomain(map, "logging", canonical.getLogging());
        flattenDomain(map, "ntp", canonical.getNtp());
        flattenDomain(map, "managementAccess", canonical.getManagementAccess());
        flattenDomain(map, "acl", canonical.getAcl());
        flattenDomain(map, "crypto", canonical.getCrypto());
        flattenDomain(map, "services", canonical.getServices());

        return map;
    }

    @SuppressWarnings("unchecked")
    private void flattenDomain(Map<String, Object> result, String prefix, Map<String, Object> domainMap) {
        if (domainMap == null || domainMap.isEmpty()) return;

        for (Map.Entry<String, Object> entry : domainMap.entrySet()) {
            String key = prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flattenDomain(result, key, (Map<String, Object>) value);
            } else if (value != null) {
                result.put(key, value);
            }
        }
    }

    private boolean areEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Boolean && b instanceof Boolean) {
            return a.equals(b);
        }
        if (isNumeric(a) && isNumeric(b)) {
            try {
                return Double.parseDouble(String.valueOf(a)) == Double.parseDouble(String.valueOf(b));
            } catch (NumberFormatException ignored) {}
        }
        return String.valueOf(a).trim().equalsIgnoreCase(String.valueOf(b).trim());
    }

    private boolean isNumeric(Object obj) {
        if (obj instanceof Number) return true;
        try {
            Double.parseDouble(String.valueOf(obj));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private DriftEvent toDomain(DriftEventDocument doc) {
        DriftEvent event = new DriftEvent();
        event.setId(doc.getId());
        event.setDeviceId(doc.getDeviceId());
        event.setFromVersionId(doc.getFromVersionId());
        event.setToVersionId(doc.getToVersionId());
        event.setFromVersion(doc.getFromVersion());
        event.setToVersion(doc.getToVersion());
        event.setChanges(doc.getChanges());
        event.setAffectedControlIds(doc.getAffectedControlIds());
        event.setAffectedFindingIds(doc.getAffectedFindingIds());
        event.setRiskBefore(doc.getRiskBefore());
        event.setRiskAfter(doc.getRiskAfter());
        event.setImpact(doc.getImpact());
        event.setDetectedAt(doc.getDetectedAt());
        event.setCreatedAt(doc.getCreatedAt());
        event.setUpdatedAt(doc.getUpdatedAt());
        return event;
    }
}
