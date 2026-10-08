package com.nexuscomply.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiDTO {
    private UUID id;
    private String rawId;
    private String status;
    private String result;

    private String syntax;
    private String vendor;
    private Integer confidence;
    private String canonicalField;
    private String suggestedValue;
    private String reason;
    private String reviewer;
    private String date;

    public AiDTO() {}

    public AiDTO(UUID id, String status, String result) {
        this.id = id;
        this.status = status;
        this.result = result;
    }

    @JsonProperty("id")
    public String getIdString() {
        return rawId != null ? rawId : (id != null ? id.toString() : null);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRawId() {
        return rawId;
    }

    public void setRawId(String rawId) {
        this.rawId = rawId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public String getSuggestedValue() {
        return suggestedValue;
    }

    public void setSuggestedValue(String suggestedValue) {
        this.suggestedValue = suggestedValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
