package com.nexuscomply.compliance.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "compliance_results")
public class ComplianceResult {
    @Id
    private String id;
    private String auditId;
    private String controlId;
    private String frameworkId;
    private String status; // PASSED, FAILED, UNKNOWN
    private String findingId;
    private String reason;
    private Instant evaluatedAt = Instant.now();

    public ComplianceResult() {}

    public ComplianceResult(String id, String auditId, String controlId, String frameworkId, String status, String findingId, String reason) {
        this.id = id;
        this.auditId = auditId;
        this.controlId = controlId;
        this.frameworkId = frameworkId;
        this.status = status;
        this.findingId = findingId;
        this.reason = reason;
        this.evaluatedAt = Instant.now();
    }

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

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
    }

    public String getFrameworkId() {
        return frameworkId;
    }

    public void setFrameworkId(String frameworkId) {
        this.frameworkId = frameworkId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFindingId() {
        return findingId;
    }

    public void setFindingId(String findingId) {
        this.findingId = findingId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
