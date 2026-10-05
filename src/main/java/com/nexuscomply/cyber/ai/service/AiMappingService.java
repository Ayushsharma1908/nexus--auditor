package com.nexuscomply.cyber.ai.service;

import com.nexuscomply.cyber.ai.persistence.AiMappingDocument;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for the AI unknown-syntax feedback loop (schema1.md section 17 & 18).
 */
public interface AiMappingService {

    /**
     * Stores an unknown configuration line in the database as PENDING_REVIEW.
     * Guaranteed: never calls any AI provider during this recording.
     */
    AiMappingDocument recordUnknownSyntax(String vendor, String platform, String rawSyntax);

    /**
     * Triggers AI proposal generation for an existing PENDING_REVIEW unknown syntax item.
     * Validates that proposed canonical field is allowlisted and value matches expected type.
     * Retains status PENDING_REVIEW (never auto-approves).
     */
    AiMappingDocument requestSuggestion(String unknownId);

    /**
     * Human approval transition: marks mapping APPROVED and records reviewer metadata.
     * Throws IllegalArgumentException if reviewerId is null or blank.
     */
    AiMappingDocument approve(String id, String reviewerId);

    /**
     * Human rejection transition: marks mapping REJECTED and records reviewer metadata.
     * Throws IllegalArgumentException if reviewerId is null or blank.
     */
    AiMappingDocument reject(String id, String reviewerId, String reason);

    Optional<AiMappingDocument> getById(String id);

    List<AiMappingDocument> getPendingReview();

    List<AiMappingDocument> getApproved(String vendor, String platform);
}
