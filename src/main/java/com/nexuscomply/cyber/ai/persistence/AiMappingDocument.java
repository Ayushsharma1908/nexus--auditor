package com.nexuscomply.cyber.ai.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representation for ai_mappings collection (schema1.md section 17).
 *
 * <p>Stores human-validated mappings for previously unknown configuration syntax.
 * AI suggests; human validates; the mapping becomes reusable.
 *
 * <p>Indexes per schema1.md section 17:
 * vendor, platform, rawSyntax, canonicalField, status, (vendor, platform, rawSyntax).
 */
@Document(collection = "ai_mappings")
@CompoundIndex(name = "vendor_platform_raw_idx", def = "{'vendor': 1, 'platform': 1, 'rawSyntax': 1}")
public class AiMappingDocument {

    @Id
    private String id;

    @Indexed
    private String vendor;

    @Indexed
    private String platform;

    @Indexed
    private String rawSyntax;

    @Indexed
    private String canonicalField;

    private Object mappedValue;

    private String unit;

    private Double confidence;

    private String reason;

    @Indexed
    private String status = "PENDING_REVIEW"; // PENDING_REVIEW, APPROVED, REJECTED

    private String suggestedBy;

    private AiReview review = new AiReview();

    private int usageCount = 0;

    private Instant createdAt;
    private Instant updatedAt;

    public AiMappingDocument() {}

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

    public String getRawSyntax() {
        return rawSyntax;
    }

    public void setRawSyntax(String rawSyntax) {
        this.rawSyntax = rawSyntax;
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public Object getMappedValue() {
        return mappedValue;
    }

    public void setMappedValue(Object mappedValue) {
        this.mappedValue = mappedValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSuggestedBy() {
        return suggestedBy;
    }

    public void setSuggestedBy(String suggestedBy) {
        this.suggestedBy = suggestedBy;
    }

    public AiReview getReview() {
        return review;
    }

    public void setReview(AiReview review) {
        this.review = review;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
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
