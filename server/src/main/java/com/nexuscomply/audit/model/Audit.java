package com.nexuscomply.audit.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "audits")
public class Audit {
    @Id
    private String id;
    private String auditNumber;
    private String name;
    private String type = "COMPREHENSIVE";
    private String status = "COMPLETED"; // QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED

    // Convenience fields aligned with frontend contract
    private String deviceId;
    private String configurationId;
    private String date;
    private String duration;

    private AuditScope scope = new AuditScope();
    private AuditSummary summary = new AuditSummary();
    private AuditRisk risk = new AuditRisk();
    private List<FrameworkResult> frameworkResults = new ArrayList<>();
    private List<String> findingIds = new ArrayList<>();
    private Instant startedAt = Instant.now();
    private Instant completedAt;

    public Audit() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuditNumber() {
        return auditNumber;
    }

    public void setAuditNumber(String auditNumber) {
        this.auditNumber = auditNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("deviceId")
    public String getDeviceId() {
        if (deviceId != null && !deviceId.isEmpty()) {
            return deviceId;
        }
        if (scope != null && scope.getDeviceIds() != null && !scope.getDeviceIds().isEmpty()) {
            return scope.getDeviceIds().get(0);
        }
        return null;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @JsonProperty("configurationId")
    public String getConfigurationId() {
        if (configurationId != null && !configurationId.isEmpty()) {
            return configurationId;
        }
        if (scope != null && scope.getConfigurationIds() != null && !scope.getConfigurationIds().isEmpty()) {
            return scope.getConfigurationIds().get(0);
        }
        return null;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    @JsonProperty("date")
    public String getDate() {
        if (date != null && !date.isEmpty()) {
            return date;
        }
        if (startedAt != null) {
            return startedAt.toString().substring(0, 10);
        }
        return null;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @JsonProperty("createdAt")
    public String getCreatedAt() {
        return startedAt != null ? startedAt.toString() : null;
    }

    @JsonProperty("duration")
    public String getDuration() {
        return duration != null ? duration : "4.2s";
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    @JsonProperty("findings")
    public Integer getFindings() {
        if (summary != null && summary.getTotalFindings() != null) {
            return summary.getTotalFindings();
        }
        return findingIds != null ? findingIds.size() : 0;
    }

    @JsonProperty("frameworks")
    public List<String> getFrameworks() {
        if (scope != null && scope.getFrameworkIds() != null && !scope.getFrameworkIds().isEmpty()) {
            return scope.getFrameworkIds();
        }
        return List.of();
    }

    @JsonProperty("compliance")
    public Integer getCompliance() {
        if (summary != null && summary.getComplianceScore() != null) {
            return summary.getComplianceScore().intValue();
        }
        return 100;
    }

    public AuditScope getScope() {
        return scope;
    }

    public void setScope(AuditScope scope) {
        this.scope = scope != null ? scope : new AuditScope();
    }

    public AuditSummary getSummary() {
        return summary;
    }

    public void setSummary(AuditSummary summary) {
        this.summary = summary != null ? summary : new AuditSummary();
    }

    public AuditRisk getRisk() {
        return risk;
    }

    public void setRisk(AuditRisk risk) {
        this.risk = risk != null ? risk : new AuditRisk();
    }

    public List<FrameworkResult> getFrameworkResults() {
        return frameworkResults;
    }

    public void setFrameworkResults(List<FrameworkResult> frameworkResults) {
        this.frameworkResults = frameworkResults != null ? frameworkResults : new ArrayList<>();
    }

    public List<String> getFindingIds() {
        return findingIds;
    }

    public void setFindingIds(List<String> findingIds) {
        this.findingIds = findingIds != null ? findingIds : new ArrayList<>();
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
}
