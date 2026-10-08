package com.nexuscomply.cyber.compliance.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexuscomply.cyber.compliance.RuleRequirement;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComplianceRule {

    private String id;
    private String controlId;
    private String frameworkId;
    private List<String> frameworkIds = new ArrayList<>();
    private List<String> baselines = new ArrayList<>();

    private String ruleCode;
    private String name;
    private String description;

    @JsonAlias({"requirement", "expression"})
    private RuleRequirement requirement;

    private String severity = "HIGH";

    private List<String> applicableVendors = new ArrayList<>();
    private List<String> applicablePlatforms = new ArrayList<>();
    private List<String> applicableOsVersions = new ArrayList<>();

    private String status = "ACTIVE";
    private int version = 1;

    public ComplianceRule() {}

    public ComplianceRule(String id, String controlId, String frameworkId, String ruleCode, RuleRequirement requirement, String severity, List<String> applicableVendors, List<String> applicablePlatforms, String status, int version) {
        this.id = id;
        this.controlId = controlId;
        this.frameworkId = frameworkId;
        if (frameworkId != null) {
            this.frameworkIds.add(frameworkId);
        }
        this.ruleCode = ruleCode;
        this.requirement = requirement;
        this.severity = severity;
        this.applicableVendors = applicableVendors != null ? applicableVendors : new ArrayList<>();
        this.applicablePlatforms = applicablePlatforms != null ? applicablePlatforms : new ArrayList<>();
        this.status = status;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        if (frameworkId != null && !this.frameworkIds.contains(frameworkId)) {
            this.frameworkIds.add(frameworkId);
        }
    }

    public List<String> getFrameworkIds() {
        return frameworkIds;
    }

    public void setFrameworkIds(List<String> frameworkIds) {
        this.frameworkIds = frameworkIds != null ? frameworkIds : new ArrayList<>();
        if (this.frameworkId == null && !this.frameworkIds.isEmpty()) {
            this.frameworkId = this.frameworkIds.get(0);
        }
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
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

    public RuleRequirement getRequirement() {
        return requirement;
    }

    public void setRequirement(RuleRequirement requirement) {
        this.requirement = requirement;
    }

    public RuleRequirement getExpression() {
        return requirement;
    }

    public void setExpression(RuleRequirement expression) {
        this.requirement = expression;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
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

    public List<String> getBaselines() {
        return baselines;
    }

    public void setBaselines(List<String> baselines) {
        this.baselines = baselines != null ? baselines : new ArrayList<>();
    }
}
