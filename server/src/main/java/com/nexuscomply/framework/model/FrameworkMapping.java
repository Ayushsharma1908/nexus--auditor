package com.nexuscomply.framework.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "framework_mappings")
public class FrameworkMapping {
    @Id
    @JsonProperty("id")
    @JsonAlias({"id", "_id"})
    private String id;

    @JsonProperty("sourceFramework")
    @JsonAlias({"sourceFramework", "source_framework", "sourceFrameworkId", "source_framework_id"})
    private String sourceFramework;

    @JsonProperty("sourceControl")
    @JsonAlias({"sourceControl", "source_control", "sourceControlId", "source_control_id"})
    private String sourceControl;

    @JsonProperty("targetFramework")
    @JsonAlias({"targetFramework", "target_framework", "targetFrameworkId", "target_framework_id"})
    private String targetFramework;

    @JsonProperty("targetControl")
    @JsonAlias({"targetControl", "target_control", "targetControlId", "target_control_id"})
    private String targetControl;

    @JsonProperty("mappingType")
    @JsonAlias({"mappingType", "mapping_type", "relationship"})
    private String mappingType = "DIRECT"; // DIRECT, RELATED, PARTIAL, SUPPORTING_EVIDENCE

    @JsonProperty("mappingVersion")
    @JsonAlias({"mappingVersion", "mapping_version", "version"})
    private String mappingVersion = "1.0.0";

    @JsonProperty("sourceReference")
    @JsonAlias({"sourceReference", "source_reference"})
    private String sourceReference;

    @JsonProperty("reviewStatus")
    @JsonAlias({"reviewStatus", "review_status"})
    private String reviewStatus = "VALIDATED"; // PENDING, VALIDATED, REJECTED

    private Double confidence;

    @JsonProperty("provenanceNotes")
    @JsonAlias({"provenanceNotes", "provenance_notes", "notes"})
    private String provenanceNotes;

    public FrameworkMapping() {}

    public FrameworkMapping(String id, String sourceFramework, String sourceControl, String targetFramework, String targetControl, String mappingType, Double confidence, String sourceReference, String reviewStatus) {
        this.id = id;
        this.sourceFramework = sourceFramework;
        this.sourceControl = sourceControl;
        this.targetFramework = targetFramework;
        this.targetControl = targetControl;
        this.mappingType = mappingType;
        this.confidence = confidence;
        this.sourceReference = sourceReference;
        this.reviewStatus = reviewStatus != null ? reviewStatus : "VALIDATED";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceFramework() {
        return sourceFramework;
    }

    public void setSourceFramework(String sourceFramework) {
        this.sourceFramework = sourceFramework;
    }

    @JsonIgnore
    public String getSourceFrameworkId() {
        return sourceFramework;
    }

    public void setSourceFrameworkId(String sourceFrameworkId) {
        this.sourceFramework = sourceFrameworkId;
    }

    public String getSourceControl() {
        return sourceControl;
    }

    public void setSourceControl(String sourceControl) {
        this.sourceControl = sourceControl;
    }

    @JsonIgnore
    public String getSourceControlId() {
        return sourceControl;
    }

    public void setSourceControlId(String sourceControlId) {
        this.sourceControl = sourceControlId;
    }

    public String getTargetFramework() {
        return targetFramework;
    }

    public void setTargetFramework(String targetFramework) {
        this.targetFramework = targetFramework;
    }

    @JsonIgnore
    public String getTargetFrameworkId() {
        return targetFramework;
    }

    public void setTargetFrameworkId(String targetFrameworkId) {
        this.targetFramework = targetFrameworkId;
    }

    public String getTargetControl() {
        return targetControl;
    }

    public void setTargetControl(String targetControl) {
        this.targetControl = targetControl;
    }

    @JsonIgnore
    public String getTargetControlId() {
        return targetControl;
    }

    public void setTargetControlId(String targetControlId) {
        this.targetControl = targetControlId;
    }

    public String getMappingType() {
        return mappingType;
    }

    public void setMappingType(String mappingType) {
        this.mappingType = mappingType;
    }

    @JsonIgnore
    public String getRelationship() {
        return mappingType;
    }

    public void setRelationship(String relationship) {
        this.mappingType = relationship;
    }

    public String getMappingVersion() {
        return mappingVersion;
    }

    public void setMappingVersion(String mappingVersion) {
        this.mappingVersion = mappingVersion;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getProvenanceNotes() {
        return provenanceNotes;
    }

    public void setProvenanceNotes(String provenanceNotes) {
        this.provenanceNotes = provenanceNotes;
    }
}
