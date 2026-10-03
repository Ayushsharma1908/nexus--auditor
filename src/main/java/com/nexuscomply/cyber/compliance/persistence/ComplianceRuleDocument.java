package com.nexuscomply.cyber.compliance.persistence;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.nexuscomply.cyber.compliance.RuleRequirement;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "compliance_rules")
public class ComplianceRuleDocument {

    @Id
    private String id;

    @Indexed
    private String ruleCode;

    @Indexed
    private String controlId;

    private String name;
    private String description;

    @JsonAlias({"expression", "requirement"})
    private RuleRequirement expression;

    private String severity = "HIGH";

    @Indexed
    private List<String> frameworkIds = new ArrayList<>();

    private List<String> baselines = new ArrayList<>();

    private List<String> applicableVendors = new ArrayList<>();
    private List<String> applicablePlatforms = new ArrayList<>();
    private List<String> applicableOsVersions = new ArrayList<>();

    @Indexed
    private String status = "ACTIVE";

    private int version = 1;

    private Instant createdAt;
    private Instant updatedAt;

    public ComplianceRuleDocument() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RuleRequirement getExpression() {
        return expression;
    }

    public void setExpression(RuleRequirement expression) {
        this.expression = expression;
    }

    public RuleRequirement getRequirement() {
        return expression;
    }

    public void setRequirement(RuleRequirement requirement) {
        this.expression = requirement;
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

    public List<String> getApplicableVendors() {
        return applicableVendors;
    }

    public void setApplicableVendors(List<String> applicableVendors) {
        this.applicableVendors = applicableVendors != null ? applicableVendors : new ArrayList<>();
    }

    public List<String> getApplicablePlatforms() {
        return applicablePlatforms;
    }

    public void setApplicablePlatforms(List<String> applicablePlatforms) {
        this.applicablePlatforms = applicablePlatforms != null ? applicablePlatforms : new ArrayList<>();
    }

    public List<String> getApplicableOsVersions() {
        return applicableOsVersions;
    }

    public void setApplicableOsVersions(List<String> applicableOsVersions) {
        this.applicableOsVersions = applicableOsVersions != null ? applicableOsVersions : new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public List<String> getBaselines() {
        return baselines;
    }

    public void setBaselines(List<String> baselines) {
        this.baselines = baselines != null ? baselines : new ArrayList<>();
    }
}
