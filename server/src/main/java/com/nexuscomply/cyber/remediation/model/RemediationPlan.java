package com.nexuscomply.cyber.remediation.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model for a remediation plan generated for a finding.
 * Field names strictly conform to schema1.md Section 16 (remediation_plans).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemediationPlan {

    private String id;

    private String findingId;
    private String deviceId;

    private String templateId;

    private String status = "PLANNED";

    private List<PlanStep> steps = new ArrayList<>();

    private PlanValidation validation = new PlanValidation();

    private PlanVerification verification = new PlanVerification();

    private String createdBy = "SYSTEM_REMEDIATION_ENGINE";

    private Instant createdAt;
    private Instant updatedAt;

    public RemediationPlan() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFindingId() {
        return findingId;
    }

    public void setFindingId(String findingId) {
        this.findingId = findingId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<PlanStep> getSteps() {
        return steps;
    }

    public void setSteps(List<PlanStep> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public PlanValidation getValidation() {
        return validation;
    }

    public void setValidation(PlanValidation validation) {
        this.validation = validation != null ? validation : new PlanValidation();
    }

    public PlanVerification getVerification() {
        return verification;
    }

    public void setVerification(PlanVerification verification) {
        this.verification = verification != null ? verification : new PlanVerification();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
