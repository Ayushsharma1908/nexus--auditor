package com.nexuscomply.finding.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "findings")
public class Finding {
    @Id
    private String id;
    private String auditId;
    private String deviceId;
    private String configurationId;
    private String versionId;
    private String controlId;
    private String control;
    private String ruleId;
    private String rule;
    private String title;
    private String description;
    private String severity = "Medium"; // Critical, High, Medium, Low
    private String status = "OPEN"; // OPEN, ACKNOWLEDGED, IN_REVIEW, REMEDIATION_PLANNED, RESOLVED, FALSE_POSITIVE
    private Integer line = 1;
    private String framework = "CIS Cisco IOS XE Benchmark";
    private String expected;
    private String actual;
    private String impact;
    private String canonicalField;
    private List<String> remediation = new ArrayList<>();
    private String remediationGuidance;
    private List<String> evidenceIds = new ArrayList<>();
    private List<FindingHistoryEntry> history = new ArrayList<>();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Finding() {}

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

    public String getControlId() {
        return controlId != null ? controlId : control;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
        if (this.control == null) this.control = controlId;
    }

    @JsonProperty("control")
    public String getControl() {
        return control != null ? control : controlId;
    }

    public void setControl(String control) {
        this.control = control;
        if (this.controlId == null) this.controlId = control;
    }

    public String getRuleId() {
        return ruleId != null ? ruleId : rule;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
        if (this.rule == null) this.rule = ruleId;
    }

    @JsonProperty("rule")
    public String getRule() {
        return rule != null ? rule : ruleId;
    }

    public void setRule(String rule) {
        this.rule = rule;
        if (this.ruleId == null) this.ruleId = rule;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getLine() {
        return line != null ? line : 1;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public List<String> getRemediation() {
        if (remediation != null && !remediation.isEmpty()) {
            return remediation;
        }
        if (remediationGuidance != null && !remediationGuidance.isBlank()) {
            return List.of(remediationGuidance);
        }
        return List.of();
    }

    public void setRemediation(List<String> remediation) {
        this.remediation = remediation != null ? remediation : new ArrayList<>();
    }

    public String getRemediationGuidance() {
        return remediationGuidance;
    }

    public void setRemediationGuidance(String remediationGuidance) {
        this.remediationGuidance = remediationGuidance;
    }

    public List<String> getEvidenceIds() {
        return evidenceIds;
    }

    public void setEvidenceIds(List<String> evidenceIds) {
        this.evidenceIds = evidenceIds != null ? evidenceIds : new ArrayList<>();
    }

    public List<FindingHistoryEntry> getHistory() {
        return history;
    }

    public void setHistory(List<FindingHistoryEntry> history) {
        this.history = history != null ? history : new ArrayList<>();
    }

    @JsonProperty("createdAt")
    public String getCreatedAtFormatted() {
        return createdAt != null ? createdAt.toString().substring(0, Math.min(10, createdAt.toString().length())) : null;
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
