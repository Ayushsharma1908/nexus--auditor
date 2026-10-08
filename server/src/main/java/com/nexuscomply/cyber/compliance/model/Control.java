package com.nexuscomply.cyber.compliance.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Control {

    private String id;
    private String frameworkId;

    @JsonAlias({"controlId", "code"})
    private String controlId;

    private String title;
    private String description;

    @JsonAlias({"category", "family"})
    private String category;

    private String severity = "HIGH";
    private String status = "ACTIVE";

    /**
     * Descriptive, human/API-facing requirements (schema1.md section 7 parity).
     * This field is denormalized and read-only for external consumers (e.g. GET /api/v1/controls/{id}).
     * ComplianceRule.requirement is the sole executable field read by the evaluator engine.
     */
    private List<ControlRequirement> requirements = new ArrayList<>();

    private List<String> baselines = new ArrayList<>();

    public Control() {}

    public Control(String id, String frameworkId, String controlId, String title, String description, String category, String severity, String status) {
        this.id = id;
        this.frameworkId = frameworkId;
        this.controlId = controlId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.severity = severity;
        this.status = status;
        this.requirements = new ArrayList<>();
    }

    public Control(String id, String frameworkId, String controlId, String title, String description, String category, String severity, String status, List<ControlRequirement> requirements) {
        this.id = id;
        this.frameworkId = frameworkId;
        this.controlId = controlId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.severity = severity;
        this.status = status;
        this.requirements = requirements != null ? requirements : new ArrayList<>();
    }

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

    public String getCode() {
        return controlId;
    }

    public void setCode(String code) {
        this.controlId = code;
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

    public String getFamily() {
        return category;
    }

    public void setFamily(String family) {
        this.category = family;
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
