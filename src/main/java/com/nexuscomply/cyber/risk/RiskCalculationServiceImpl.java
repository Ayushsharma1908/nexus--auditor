package com.nexuscomply.cyber.risk;

import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.finding.Finding;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentDocument;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of RiskCalculationService implementing the 5-factor risk scoring formula
 * per cyberlayer.pdf section 18 and schema1.md section 12.
 *
 * <p>Scoring Formula Breakdown:
 * <pre>
 * Factor 1: Finding Severity (Weight = 35%)
 *   - CRITICAL = 100.0, HIGH = 80.0, MEDIUM = 50.0, LOW = 20.0
 *   - Contribution = normalizedSeverity * 0.35
 *
 * Factor 2: Asset Criticality (Weight = 20%)
 *   - CRITICAL = 100.0, HIGH = 80.0, MEDIUM = 50.0 (default), LOW = 20.0
 *   - Contribution = normalizedAssetCriticality * 0.20
 *   - Sourced as PROVIDED or DEFAULT_PLACEHOLDER
 *
 * Factor 3: Network Exposure (Weight = 20%)
 *   - EXTERNAL/PERIMETER/INTERNET = 100.0, DMZ = 75.0, INTERNAL = 50.0 (default), ISOLATED = 20.0
 *   - Contribution = normalizedNetworkExposure * 0.20
 *   - Sourced as PROVIDED or DEFAULT_PLACEHOLDER
 *
 * Factor 4: Control Weight (Weight = 15%)
 *   - Derived from ComplianceRule.severity as documented proxy (Absolute Rule 6):
 *     CRITICAL = 100.0, HIGH = 80.0, MEDIUM = 50.0, LOW = 20.0
 *   - Contribution = normalizedControlWeight * 0.15
 *
 * Factor 5: Confidence / Uncertainty (Weight = 10%)
 *   - Real vendor detection confidence (0.0 - 1.0) scaled to 0 - 100.0
 *   - Contribution = (detectionConfidence * 100.0) * 0.10
 *
 * Final Score = Math.round(Sum of 5 contributions) in [0, 100]
 * Risk Level:
 *   - Score >= 85: CRITICAL
 *   - Score >= 65: HIGH
 *   - Score >= 40: MEDIUM
 *   - Score <  40: LOW
 * </pre>
 */
@Service
public class RiskCalculationServiceImpl implements RiskCalculationService {

    private static final Logger log = LoggerFactory.getLogger(RiskCalculationServiceImpl.class);

    private static final double WEIGHT_SEVERITY = 0.35;
    private static final double WEIGHT_ASSET_CRITICALITY = 0.20;
    private static final double WEIGHT_NETWORK_EXPOSURE = 0.20;
    private static final double WEIGHT_CONTROL_WEIGHT = 0.15;
    private static final double WEIGHT_CONFIDENCE = 0.10;

    private final RiskAssessmentRepository riskAssessmentRepository;

    public RiskCalculationServiceImpl(RiskAssessmentRepository riskAssessmentRepository) {
        this.riskAssessmentRepository = riskAssessmentRepository;
    }

    @Override
    public RiskAssessment calculateAndPersistRisk(Finding finding, RiskContext context) {
        return calculateAndPersistRisk(finding, null, context);
    }

    @Override
    public RiskAssessment calculateAndPersistRisk(Finding finding, ComplianceRule rule, RiskContext context) {
        RiskAssessment assessment = calculateRisk(finding, rule, context);
        RiskAssessmentDocument doc = toDocument(assessment);
        RiskAssessmentDocument saved = riskAssessmentRepository.save(doc);
        log.info("Persisted RiskAssessment [{}] for Finding [{}] with score [{}] and level [{}].",
                saved.getId(), finding.getId(), saved.getScore(), saved.getLevel());
        return toDomain(saved);
    }

    @Override
    public RiskAssessment calculateRisk(Finding finding, RiskContext context) {
        return calculateRisk(finding, null, context);
    }

    @Override
    public RiskAssessment calculateRisk(Finding finding, ComplianceRule rule, RiskContext context) {
        if (finding == null) {
            throw new IllegalArgumentException("Finding cannot be null for risk calculation.");
        }
        RiskContext ctx = context != null ? context : new RiskContext();

        // 1. Finding Severity (35%)
        String rawSeverity = finding.getSeverity() != null ? finding.getSeverity().toUpperCase() : "MEDIUM";
        double normSeverity = normalizeSeverity(rawSeverity);
        double contribSeverity = roundToTwoDecimals(normSeverity * WEIGHT_SEVERITY);

        // 2. Asset Criticality (20%)
        String rawAssetCrit = ctx.getAssetCriticality() != null ? ctx.getAssetCriticality().toUpperCase() : RiskContext.DEFAULT_ASSET_CRITICALITY;
        double normAssetCrit = normalizeSeverity(rawAssetCrit);
        double contribAssetCrit = roundToTwoDecimals(normAssetCrit * WEIGHT_ASSET_CRITICALITY);

        // 3. Network Exposure (20%)
        String rawExposure = ctx.getNetworkExposure() != null ? ctx.getNetworkExposure().toUpperCase() : RiskContext.DEFAULT_NETWORK_EXPOSURE;
        double normExposure = normalizeExposure(rawExposure);
        double contribExposure = roundToTwoDecimals(normExposure * WEIGHT_NETWORK_EXPOSURE);

        // 4. Control Importance / Weight (15%) - derived from rule severity proxy
        String rawControlWeight;
        if (rule != null && rule.getSeverity() != null) {
            rawControlWeight = rule.getSeverity().toUpperCase();
        } else {
            rawControlWeight = rawSeverity;
        }
        double normControlWeight = normalizeSeverity(rawControlWeight);
        double contribControlWeight = roundToTwoDecimals(normControlWeight * WEIGHT_CONTROL_WEIGHT);

        // 5. Confidence / Uncertainty (10%) - real vendor detection confidence
        double rawConfidence = ctx.getDetectionConfidence();
        if (rawConfidence < 0.0) rawConfidence = 0.0;
        if (rawConfidence > 1.0) rawConfidence = 1.0;
        double normConfidence = roundToTwoDecimals(rawConfidence * 100.0);
        double contribConfidence = roundToTwoDecimals(normConfidence * WEIGHT_CONFIDENCE);

        // Total score calculation (sum of contributions)
        double totalWeightedScore = contribSeverity + contribAssetCrit + contribExposure + contribControlWeight + contribConfidence;
        int finalScore = (int) Math.round(totalWeightedScore);
        if (finalScore < 0) finalScore = 0;
        if (finalScore > 100) finalScore = 100;

        String level = mapScoreToLevel(finalScore);

        // Factor values on schema-compatible 1-10 scale
        RiskFactors factors = new RiskFactors();
        factors.setSeverity(roundToOneDecimal(normSeverity / 10.0));
        factors.setAssetCriticality(roundToOneDecimal(normAssetCrit / 10.0));
        factors.setExposure(roundToOneDecimal(normExposure / 10.0));
        // Named 'exploitability' per schema1.md section 12, conceptually holding control importance / weight proxy
        factors.setExploitability(roundToOneDecimal(normControlWeight / 10.0));
        factors.setConfidence(roundToOneDecimal(normConfidence / 10.0));

        // Preserve raw inputs
        Map<String, Object> rawInputs = new LinkedHashMap<>();
        rawInputs.put("severity", rawSeverity);
        rawInputs.put("assetCriticality", rawAssetCrit);
        rawInputs.put("networkExposure", rawExposure);
        rawInputs.put("exploitability", rawControlWeight);
        rawInputs.put("confidence", rawConfidence);
        factors.setRawInputs(rawInputs);

        // Preserve individual factor contributions
        Map<String, Double> contributions = new LinkedHashMap<>();
        contributions.put("severity", contribSeverity);
        contributions.put("assetCriticality", contribAssetCrit);
        contributions.put("exposure", contribExposure);
        contributions.put("exploitability", contribControlWeight);
        contributions.put("confidence", contribConfidence);
        factors.setContributions(contributions);

        Instant now = Instant.now();
        RiskAssessment assessment = new RiskAssessment();
        assessment.setId(UUID.randomUUID().toString());
        assessment.setAuditId(finding.getAuditId());
        assessment.setDeviceId(finding.getDeviceId());
        assessment.setFindingId(finding.getId());
        assessment.setScore(finalScore);
        assessment.setLevel(level);
        assessment.setFactors(factors);
        assessment.setCalculationVersion("1.0");

        assessment.setAssetCriticalitySource(ctx.getAssetCriticalitySource());
        assessment.setNetworkExposureSource(ctx.getNetworkExposureSource());
        assessment.setConfidence(rawConfidence);
        assessment.setConfidenceSource("VENDOR_DETECTION");

        assessment.setCalculatedAt(now);
        assessment.setCreatedAt(now);
        assessment.setUpdatedAt(now);

        return assessment;
    }

    private double normalizeSeverity(String severity) {
        if (severity == null) return 50.0;
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> 100.0;
            case "HIGH" -> 80.0;
            case "MEDIUM" -> 50.0;
            case "LOW" -> 20.0;
            default -> 50.0;
        };
    }

    private double normalizeExposure(String exposure) {
        if (exposure == null) return 50.0;
        return switch (exposure.toUpperCase()) {
            case "PERIMETER", "EXTERNAL", "INTERNET" -> 100.0;
            case "DMZ" -> 75.0;
            case "INTERNAL" -> 50.0;
            case "ISOLATED", "MANAGEMENT_ONLY" -> 20.0;
            default -> 50.0;
        };
    }

    private String mapScoreToLevel(int score) {
        if (score >= 85) return "CRITICAL";
        if (score >= 65) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    private double roundToOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private double roundToTwoDecimals(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private RiskAssessmentDocument toDocument(RiskAssessment domain) {
        RiskAssessmentDocument doc = new RiskAssessmentDocument();
        doc.setId(domain.getId());
        doc.setAuditId(domain.getAuditId());
        doc.setDeviceId(domain.getDeviceId());
        doc.setFindingId(domain.getFindingId());
        doc.setScore(domain.getScore());
        doc.setLevel(domain.getLevel());
        doc.setFactors(domain.getFactors());
        doc.setCalculationVersion(domain.getCalculationVersion());
        doc.setAssetCriticalitySource(domain.getAssetCriticalitySource());
        doc.setNetworkExposureSource(domain.getNetworkExposureSource());
        doc.setConfidence(domain.getConfidence());
        doc.setConfidenceSource(domain.getConfidenceSource());
        doc.setCalculatedAt(domain.getCalculatedAt());
        doc.setCreatedAt(domain.getCreatedAt());
        doc.setUpdatedAt(domain.getUpdatedAt());
        return doc;
    }

    private RiskAssessment toDomain(RiskAssessmentDocument doc) {
        RiskAssessment domain = new RiskAssessment();
        domain.setId(doc.getId());
        domain.setAuditId(doc.getAuditId());
        domain.setDeviceId(doc.getDeviceId());
        domain.setFindingId(doc.getFindingId());
        domain.setScore(doc.getScore());
        domain.setLevel(doc.getLevel());
        domain.setFactors(doc.getFactors());
        domain.setCalculationVersion(doc.getCalculationVersion());
        domain.setAssetCriticalitySource(doc.getAssetCriticalitySource());
        domain.setNetworkExposureSource(doc.getNetworkExposureSource());
        domain.setConfidence(doc.getConfidence());
        domain.setConfidenceSource(doc.getConfidenceSource());
        domain.setCalculatedAt(doc.getCalculatedAt());
        domain.setCreatedAt(doc.getCreatedAt());
        domain.setUpdatedAt(doc.getUpdatedAt());
        return domain;
    }
}
