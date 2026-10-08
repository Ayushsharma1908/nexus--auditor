package com.nexuscomply.cyber.finding.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document representation for findings collection (schema1.md section 10).
 *
 * <p>Field names, nesting, and types match schema1.md section 10 exactly.
 * Risk score is deliberately excluded in this task per absolute rule 2.
 */
@Document(collection = "findings")
@CompoundIndexes({
        @CompoundIndex(name = "device_status_idx", def = "{'deviceId': 1, 'status': 1}"),
        @CompoundIndex(name = "audit_status_idx", def = "{'auditId': 1, 'status': 1}")
})
public class FindingDocument {

    @Id
    private String id;

    @Indexed
    private String auditId;

    @Indexed
    private String deviceId;

    private String configurationId;

    @Indexed
    private String controlId;

    @Indexed
    private String ruleId;

    private String controlCode;
    private String title;
    private String description;

    @Indexed
    private String status = "OPEN";

    private String complianceStatus = "FAIL";

    @Indexed
    private String severity;

    @Indexed
    private List<String> frameworkIds = new ArrayList<>();

    private String canonicalField;
    private Object expected;
    private Object actual;

    private String impact;
    private List<String> evidenceIds = new ArrayList<>();

    private boolean remediationAvailable = true;

    @Indexed
    private Instant createdAt;

    private Instant updatedAt;

    public FindingDocument() {}

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

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getControlCode() {
        return controlCode;
    }

    public void setControlCode(String controlCode) {
        this.controlCode = controlCode;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public List<String> getFrameworkIds() {
        return frameworkIds;
    }

    public void setFrameworkIds(List<String> frameworkIds) {
        this.frameworkIds = frameworkIds != null ? frameworkIds : new ArrayList<>();
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public Object getExpected() {
        return expected;
    }

    public void setExpected(Object expected) {
        this.expected = expected;
    }

    public Object getActual() {
        return actual;
    }

    public void setActual(Object actual) {
        this.actual = actual;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public List<String> getEvidenceIds() {
        return evidenceIds;
    }

    public void setEvidenceIds(List<String> evidenceIds) {
        this.evidenceIds = evidenceIds != null ? evidenceIds : new ArrayList<>();
    }

    public boolean isRemediationAvailable() {
        return remediationAvailable;
    }

    public void setRemediationAvailable(boolean remediationAvailable) {
        this.remediationAvailable = remediationAvailable;
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
