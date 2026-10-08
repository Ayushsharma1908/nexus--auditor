package com.nexuscomply.framework.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "controls")
public class Control {
    @Id
    @JsonProperty("id")
    @JsonAlias({"id", "_id"})
    private String id;

    @JsonProperty("frameworkId")
    @JsonAlias({"frameworkId", "framework_id"})
    private String frameworkId;

    @JsonProperty("controlId")
    @JsonAlias({"controlId", "control_id", "controlCode", "control_code"})
    private String controlId;

    @JsonProperty("controlCode")
    @JsonAlias({"controlCode", "control_code"})
    private String controlCode;

    private String title;
    private String description;
    private String severity;

    @JsonProperty("family")
    @JsonAlias({"family", "category"})
    private String family;

    private String category;
    private String version;

    @JsonProperty("sourceReference")
    @JsonAlias({"sourceReference", "source_reference"})
    private String sourceReference;

    @JsonProperty("sourceUrl")
    @JsonAlias({"sourceUrl", "source_url"})
    private String sourceUrl;

    private String status = "ACTIVE";
    private String provenance;

    @JsonProperty("remediationGuidance")
    @JsonAlias({"remediationGuidance", "remediation_guidance"})
    private String remediationGuidance;

    @JsonProperty("ruleIds")
    @JsonAlias({"ruleIds", "rule_ids"})
    private List<String> ruleIds = new ArrayList<>();

    public Control() {}

    public Control(String id, String frameworkId, String controlCode, String title, String description, String severity, String category, String remediationGuidance, List<String> ruleIds) {
        this.id = id;
        this.frameworkId = frameworkId;
        this.controlCode = controlCode;
        this.controlId = controlCode;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.category = category;
        this.family = category;
        this.remediationGuidance = remediationGuidance;
        this.ruleIds = ruleIds != null ? ruleIds : new ArrayList<>();
        this.status = "ACTIVE";
        this.sourceReference = "Authoritative source reference for " + controlCode;
        this.provenance = "Derived from authoritative benchmark standards";
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
        return controlId != null ? controlId : controlCode;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
        if (this.controlCode == null) {
            this.controlCode = controlId;
        }
    }

    @JsonIgnore
    public String getControlCode() {
        return controlCode != null ? controlCode : controlId;
    }

    public void setControlCode(String controlCode) {
        this.controlCode = controlCode;
        if (this.controlId == null) {
            this.controlId = controlCode;
        }
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

    public String getFamily() {
        return family != null ? family : category;
    }

    public void setFamily(String family) {
        this.family = family;
        if (this.category == null) {
            this.category = family;
        }
    }

    @JsonIgnore
    public String getCategory() {
        return category != null ? category : family;
    }

    public void setCategory(String category) {
        this.category = category;
        if (this.family == null) {
            this.family = category;
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public String getRemediationGuidance() {
        return remediationGuidance;
    }

    public void setRemediationGuidance(String remediationGuidance) {
        this.remediationGuidance = remediationGuidance;
    }

    public List<String> getRuleIds() {
        return ruleIds;
    }

    public void setRuleIds(List<String> ruleIds) {
        this.ruleIds = ruleIds != null ? ruleIds : new ArrayList<>();
    }
}
