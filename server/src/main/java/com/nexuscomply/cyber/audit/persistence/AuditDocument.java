package com.nexuscomply.cyber.audit.persistence;

import com.nexuscomply.cyber.audit.AuditProgress;
import com.nexuscomply.cyber.audit.AuditSummary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document representation of a compliance audit execution.
 * Collection: "audits" per schema1.md section 9.
 *
 * <p>Indexes per schema1.md section 9:
 * deviceId, configurationId, versionId, status, createdAt, (deviceId, createdAt).
 */
@Document(collection = "audits")
@CompoundIndex(name = "device_created_idx", def = "{'deviceId': 1, 'createdAt': 1}")
public class AuditDocument {

    @Id
    private String id;

    @Indexed
    private String deviceId;

    @Indexed
    private String configurationId;

    @Indexed
    private String versionId;

    private String normalizedConfigurationId;

    private List<String> frameworkIds = new ArrayList<>();

    @Indexed
    private String status;

    private AuditProgress progress;

    private AuditSummary summary;

    private Double complianceScore;

    private Instant startedAt;

    private Instant completedAt;

    private String createdBy;

    private String errorMessage;

    @Indexed
    private Instant createdAt;

    private Instant updatedAt;

    public AuditDocument() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getNormalizedConfigurationId() {
        return normalizedConfigurationId;
    }

    public void setNormalizedConfigurationId(String normalizedConfigurationId) {
        this.normalizedConfigurationId = normalizedConfigurationId;
    }

    public List<String> getFrameworkIds() {
        return frameworkIds;
    }

    public void setFrameworkIds(List<String> frameworkIds) {
        this.frameworkIds = frameworkIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public AuditProgress getProgress() {
        return progress;
    }

    public void setProgress(AuditProgress progress) {
        this.progress = progress;
    }

    public AuditSummary getSummary() {
        return summary;
    }

    public void setSummary(AuditSummary summary) {
        this.summary = summary;
    }

    public Double getComplianceScore() {
        return complianceScore;
    }

    public void setComplianceScore(Double complianceScore) {
        this.complianceScore = complianceScore;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
