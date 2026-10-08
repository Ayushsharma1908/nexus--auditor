package com.nexuscomply.cyber.audit;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a compliance audit execution.
 * Field names match schema1.md section 9 exactly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Audit {

    private String id;
    private String deviceId;
    private String configurationId;
    private String versionId;
    private String normalizedConfigurationId;

    private List<String> frameworkIds = new ArrayList<>();

    private String status = AuditStatus.QUEUED.name();
    private AuditProgress progress = new AuditProgress("QUEUED", 0);
    private AuditSummary summary = new AuditSummary();

    private Double complianceScore;

    private Instant startedAt;
    private Instant completedAt;

    private String createdBy;
    private String errorMessage;

    private Instant createdAt;
    private Instant updatedAt;

    public Audit() {}

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
