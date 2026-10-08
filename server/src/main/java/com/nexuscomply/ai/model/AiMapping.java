package com.nexuscomply.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "ai_mappings")
public class AiMapping {

    @Id
    private String id;

    private String syntax;
    private String vendor;
    private Integer confidence = 90;
    private String canonicalField;
    private String suggestedValue;
    private String reason;
    private String status = "Needs review"; // Needs review, Approved, Rejected
    private String reviewer;
    private String date;

    private String result;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public AiMapping() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        return confidence != null ? confidence : 90;
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

    public String getStatus() {
        return status != null ? status : "Needs review";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    @JsonProperty("date")
    public String getDate() {
        if (date != null && !date.isEmpty()) return date;
        if (createdAt != null) return createdAt.toString().substring(0, 10);
        return null;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
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
