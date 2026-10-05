package com.nexuscomply.cyber.ai.persistence;

import java.time.Instant;

/**
 * Review metadata sub-document for ai_mappings (schema1.md section 17).
 */
public class AiReview {

    private String reviewerId;
    private Instant reviewedAt;
    private String comment;

    public AiReview() {}

    public AiReview(String reviewerId, Instant reviewedAt, String comment) {
        this.reviewerId = reviewerId;
        this.reviewedAt = reviewedAt;
        this.comment = comment;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
