package com.nexuscomply.framework.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "compliance_rules")
public class ComplianceRule {
    @Id
    @JsonProperty("_id")
    @JsonAlias({"id", "_id"})
    private String id;

    @JsonProperty("frameworkId")
    @JsonAlias({"frameworkId", "framework_id"})
    private String frameworkId;

    @JsonProperty("controlId")
    @JsonAlias({"controlId", "control_id", "officialControlId", "official_control_id"})
    private String controlId;

    @JsonProperty("ruleName")
    @JsonAlias({"ruleName", "rule_name", "ruleIntent", "rule_intent"})
    private String ruleName;

    private String description;

    @JsonProperty("canonicalField")
    @JsonAlias({"canonicalField", "canonical_field", "targetPath", "target_path"})
    private String canonicalField;

    private String operator;

    @JsonProperty("expectedValue")
    @JsonAlias({"expectedValue", "expected_value"})
    private Object expectedValue;

    @JsonProperty("expectedType")
    @JsonAlias({"expectedType", "expected_type"})
    private String expectedType;

    private String severity;
    private String status = "ACTIVE";

    @JsonProperty("sourceType")
    @JsonAlias({"sourceType", "source_type"})
    private String sourceType = "DERIVED_TEMPLATE";

    @JsonProperty("ruleVersion")
    @JsonAlias({"ruleVersion", "rule_version", "version"})
    private String ruleVersion = "1.0.0";

    private String provenance;

    @JsonProperty("unknownHandling")
    @JsonAlias({"unknownHandling", "unknown_handling"})
    private String unknownHandling = "FLAG_UNKNOWN";

    private String applicability;
    private String notes;

    public ComplianceRule() {}

    public ComplianceRule(String id, String controlId, String ruleName, String description, String targetPath, String operator, Object expectedValue, String severity) {
        this.id = id;
        this.controlId = controlId;
        this.ruleName = ruleName;
        this.description = description;
        this.canonicalField = targetPath;
        this.operator = operator;
        this.expectedValue = expectedValue;
        this.severity = severity;
        this.status = "ACTIVE";
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

    @JsonIgnore
    public String getOfficialControlId() {
        return controlId;
    }

    public void setOfficialControlId(String officialControlId) {
        this.controlId = officialControlId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    @JsonIgnore
    public String getRuleIntent() {
        return ruleName;
    }

    public void setRuleIntent(String ruleIntent) {
        this.ruleName = ruleIntent;
    }

    public String getDescription() {
        return description != null ? description : ruleName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    @JsonIgnore
    public String getTargetPath() {
        return canonicalField;
    }

    public void setTargetPath(String targetPath) {
        this.canonicalField = targetPath;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(Object expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getExpectedType() {
        return expectedType;
    }

    public void setExpectedType(String expectedType) {
        this.expectedType = expectedType;
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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public String getUnknownHandling() {
        return unknownHandling;
    }

    public void setUnknownHandling(String unknownHandling) {
        this.unknownHandling = unknownHandling;
    }

    public String getApplicability() {
        return applicability;
    }

    public void setApplicability(String applicability) {
        this.applicability = applicability;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
