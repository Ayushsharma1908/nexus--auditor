package com.nexuscomply.framework.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "vendor_knowledge")
public class VendorKnowledge {
    @Id
    @JsonProperty("id")
    @JsonAlias({"id", "_id"})
    private String id;

    private String vendor;
    private String platform;

    @JsonProperty("platformVersionRange")
    @JsonAlias({"platformVersionRange", "platform_version_range"})
    private String platformVersionRange;

    @JsonProperty("rawSyntaxPattern")
    @JsonAlias({"rawSyntaxPattern", "raw_syntax_pattern"})
    private String rawSyntaxPattern;

    @JsonProperty("canonicalField")
    @JsonAlias({"canonicalField", "canonical_field"})
    private String canonicalField;

    @JsonProperty("canonicalValue")
    @JsonAlias({"canonicalValue", "canonical_value"})
    private String canonicalValue;

    @JsonProperty("canonicalType")
    @JsonAlias({"canonicalType", "canonical_type"})
    private String canonicalType = "STRING";

    @JsonProperty("sourceReference")
    @JsonAlias({"sourceReference", "source_reference", "source_note", "sourceNote"})
    private String sourceReference;

    @JsonProperty("sourceVersion")
    @JsonAlias({"sourceVersion", "source_version"})
    private String sourceVersion;

    @JsonProperty("validationStatus")
    @JsonAlias({"validationStatus", "validation_status"})
    private String validationStatus = "VALIDATED";

    @JsonProperty("applicabilityNotes")
    @JsonAlias({"applicabilityNotes", "applicability_notes", "notes"})
    private String applicabilityNotes;

    public VendorKnowledge() {}

    public VendorKnowledge(String id, String vendor, String platform, String rawSyntaxPattern, String canonicalField, String canonicalValue, String sourceNote) {
        this.id = id;
        this.vendor = vendor;
        this.platform = platform;
        this.rawSyntaxPattern = rawSyntaxPattern;
        this.canonicalField = canonicalField;
        this.canonicalValue = canonicalValue;
        this.sourceReference = sourceNote;
        this.applicabilityNotes = sourceNote;
        this.validationStatus = "VALIDATED";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPlatformVersionRange() {
        return platformVersionRange;
    }

    public void setPlatformVersionRange(String platformVersionRange) {
        this.platformVersionRange = platformVersionRange;
    }

    public String getRawSyntaxPattern() {
        return rawSyntaxPattern;
    }

    public void setRawSyntaxPattern(String rawSyntaxPattern) {
        this.rawSyntaxPattern = rawSyntaxPattern;
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public String getCanonicalValue() {
        return canonicalValue;
    }

    public void setCanonicalValue(String canonicalValue) {
        this.canonicalValue = canonicalValue;
    }

    public String getCanonicalType() {
        return canonicalType;
    }

    public void setCanonicalType(String canonicalType) {
        this.canonicalType = canonicalType;
    }

    public String getSourceReference() {
        // Fall back to applicabilityNotes for records persisted before sourceReference was introduced
        return sourceReference != null ? sourceReference : applicabilityNotes;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
        if (this.applicabilityNotes == null) {
            this.applicabilityNotes = sourceReference;
        }
    }

    @JsonIgnore
    public String getSourceNote() {
        return getSourceReference();
    }

    public void setSourceNote(String sourceNote) {
        this.sourceReference = sourceNote;
        if (this.applicabilityNotes == null) {
            this.applicabilityNotes = sourceNote;
        }
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getApplicabilityNotes() {
        return applicabilityNotes != null ? applicabilityNotes : sourceReference;
    }

    public void setApplicabilityNotes(String applicabilityNotes) {
        this.applicabilityNotes = applicabilityNotes;
    }
}
