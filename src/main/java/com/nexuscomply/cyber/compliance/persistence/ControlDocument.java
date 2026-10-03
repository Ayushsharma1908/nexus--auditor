package com.nexuscomply.cyber.compliance.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.nexuscomply.cyber.compliance.model.ControlRequirement;

@Document(collection = "controls")
@CompoundIndex(name = "framework_control_idx", def = "{'frameworkId': 1, 'controlId': 1}")
public class ControlDocument {

    @Id
    private String id;

    @Indexed
    private String frameworkId;

    @Indexed
    private String controlId;

    private String title;
    private String description;
    private String category;
    private String severity = "HIGH";

    @Indexed
    private String status = "ACTIVE";

    /**
     * Descriptive, human/API-facing requirements (schema1.md section 7 parity).
     * This field is denormalized and read-only for external consumers (e.g. GET /api/v1/controls/{id}).
     * ComplianceRule.requirement is the sole executable field read by the evaluator engine.
     */
    private List<ControlRequirement> requirements = new ArrayList<>();

    private List<String> baselines = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    public ControlDocument() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrameworkId() {
        return frameworkId;
    }

    public void setFrameworkId(String frameworkId) {
        this.frameworkId = frameworkId;
    }

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public List<ControlRequirement> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<ControlRequirement> requirements) {
        this.requirements = requirements != null ? requirements : new ArrayList<>();
    }

    public List<String> getBaselines() {
        return baselines;
    }

    public void setBaselines(List<String> baselines) {
        this.baselines = baselines != null ? baselines : new ArrayList<>();
    }
}
