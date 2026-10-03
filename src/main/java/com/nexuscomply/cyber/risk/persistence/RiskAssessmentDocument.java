package com.nexuscomply.cyber.risk.persistence;

import com.nexuscomply.cyber.risk.RiskFactors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representation of a risk assessment calculation.
 * Collection: "risk_assessments" per schema1.md section 12.
 *
 * <p>Indexes per schema1.md section 12:
 * auditId, deviceId, findingId, calculatedAt, (deviceId, calculatedAt).
 */
@Document(collection = "risk_assessments")
@CompoundIndex(name = "device_calculated_idx", def = "{'deviceId': 1, 'calculatedAt': 1}")
public class RiskAssessmentDocument {

    @Id
    private String id;

    @Indexed
    private String auditId;

    @Indexed
    private String deviceId;

    @Indexed
    private String findingId;

    private Integer score;
    private String level;

    private RiskFactors factors;

    private String calculationVersion = "1.0";

    private String assetCriticalitySource;
    private String networkExposureSource;
    private Double confidence;
    private String confidenceSource;

    @Indexed
    private Instant calculatedAt;

    private Instant createdAt;
    private Instant updatedAt;

    public RiskAssessmentDocument() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getFindingId() {
        return findingId;
    }

    public void setFindingId(String findingId) {
        this.findingId = findingId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public RiskFactors getFactors() {
        return factors;
    }

    public void setFactors(RiskFactors factors) {
        this.factors = factors;
    }

    public String getCalculationVersion() {
        return calculationVersion;
    }

    public void setCalculationVersion(String calculationVersion) {
        this.calculationVersion = calculationVersion;
    }

    public String getAssetCriticalitySource() {
        return assetCriticalitySource;
    }

    public void setAssetCriticalitySource(String assetCriticalitySource) {
        this.assetCriticalitySource = assetCriticalitySource;
    }

    public String getNetworkExposureSource() {
        return networkExposureSource;
    }

    public void setNetworkExposureSource(String networkExposureSource) {
        this.networkExposureSource = networkExposureSource;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getConfidenceSource() {
        return confidenceSource;
    }

    public void setConfidenceSource(String confidenceSource) {
        this.confidenceSource = confidenceSource;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
