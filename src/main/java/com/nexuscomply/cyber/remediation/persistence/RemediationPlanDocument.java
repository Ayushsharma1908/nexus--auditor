package com.nexuscomply.cyber.remediation.persistence;

import com.nexuscomply.cyber.remediation.model.PlanStep;
import com.nexuscomply.cyber.remediation.model.PlanValidation;
import com.nexuscomply.cyber.remediation.model.PlanVerification;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "remediation_plans")
@CompoundIndexes({
        @CompoundIndex(name = "finding_status_idx", def = "{'findingId': 1, 'status': 1}")
})
public class RemediationPlanDocument {

    @Id
    private String id;

    @Indexed
    private String findingId;

    @Indexed
    private String deviceId;

    @Indexed
    private String templateId;

    @Indexed
    private String status = "PLANNED";

    private List<PlanStep> steps = new ArrayList<>();

    private PlanValidation validation = new PlanValidation();

    private PlanVerification verification = new PlanVerification();

    private String createdBy = "SYSTEM_REMEDIATION_ENGINE";

    private Instant createdAt;
    private Instant updatedAt;

    public RemediationPlanDocument() {}

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
