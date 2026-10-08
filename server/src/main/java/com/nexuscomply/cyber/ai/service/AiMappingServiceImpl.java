package com.nexuscomply.cyber.ai.service;

import com.nexuscomply.cyber.ai.SecretSanitizer;
import com.nexuscomply.cyber.ai.SyntaxNoiseFilter;
import com.nexuscomply.cyber.ai.persistence.AiJobDocument;
import com.nexuscomply.cyber.ai.persistence.AiJobRepository;
import com.nexuscomply.cyber.ai.persistence.AiMappingDocument;
import com.nexuscomply.cyber.ai.persistence.AiMappingRepository;
import com.nexuscomply.cyber.ai.persistence.AiReview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of {@link AiMappingService}.
 * Strictly enforces schema1.md section 17 & 18 rules:
 * - Unknown lines are stored as PENDING_REVIEW; no provider calls during audits.
 * - Only APPROVED mappings are reusable; no code path may auto-approve.
 * - All suggestions must match CanonicalSecurityModel allowlist and pass type validation.
 * - Reviewer ID must be non-blank.
 */
@Service("cyberAiMappingService")
public class AiMappingServiceImpl implements AiMappingService {

    private static final Logger log = LoggerFactory.getLogger(AiMappingServiceImpl.class);

    private final AiMappingRepository aiMappingRepository;
    private final AiJobRepository aiJobRepository;
    private final SuggestionProvider suggestionProvider;

    @Autowired
    public AiMappingServiceImpl(
            AiMappingRepository aiMappingRepository,
            AiJobRepository aiJobRepository,
            SuggestionProvider suggestionProvider) {
        this.aiMappingRepository = aiMappingRepository;
        this.aiJobRepository = aiJobRepository;
        this.suggestionProvider = suggestionProvider;
    }

    @Override
    public AiMappingDocument recordUnknownSyntax(String vendor, String platform, String rawSyntax) {
        if (rawSyntax == null || rawSyntax.isBlank()) {
            return null;
        }

        String trimmed = rawSyntax.trim();
        if (SyntaxNoiseFilter.isNoise(trimmed)) {
            return null;
        }

        String sanitized = SecretSanitizer.sanitize(trimmed);

        Optional<AiMappingDocument> existing = aiMappingRepository.findByVendorAndPlatformAndRawSyntax(vendor, platform, sanitized);
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant now = Instant.now();
        AiMappingDocument doc = new AiMappingDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setVendor(vendor);
        doc.setPlatform(platform);
        doc.setRawSyntax(sanitized);
        doc.setStatus("PENDING_REVIEW");
        doc.setUsageCount(0);
        doc.setReview(new AiReview(null, null, null));
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);

        return aiMappingRepository.save(doc);
    }

    @Override
    public AiMappingDocument requestSuggestion(String unknownId) {
        AiMappingDocument doc = aiMappingRepository.findById(unknownId)
                .orElseThrow(() -> new NoSuchElementException("Unknown syntax mapping not found for ID: " + unknownId));

        Instant now = Instant.now();
        AiJobDocument job = new AiJobDocument();
        job.setId(UUID.randomUUID().toString());
        job.setType("UNKNOWN_SYNTAX_ANALYSIS");
        job.setStatus("RUNNING");
        job.setStartedAt(now);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        String sanitizedSyntax = SecretSanitizer.sanitize(doc.getRawSyntax());
        Map<String, Object> inputMap = new LinkedHashMap<>();
        inputMap.put("unknownCount", 1);
        inputMap.put("rawSyntax", sanitizedSyntax);
        inputMap.put("vendor", doc.getVendor());
        inputMap.put("platform", doc.getPlatform());
        job.setInput(inputMap);

        job = aiJobRepository.save(job);

        try {
            SuggestionResult suggestion = suggestionProvider.propose(
                    doc.getVendor(),
                    doc.getPlatform(),
                    sanitizedSyntax,
                    CanonicalFieldAllowlist.getAllowedFields()
            );

            if (suggestion != null) {
                // Strict Allowlist and Type Validation
                CanonicalFieldAllowlist.validateFieldAndValue(
                        suggestion.getCanonicalField(),
                        suggestion.getMappedValue()
                );

                doc.setCanonicalField(suggestion.getCanonicalField());
                doc.setMappedValue(suggestion.getMappedValue());
                doc.setUnit(suggestion.getUnit());
                doc.setConfidence(suggestion.getConfidence());
                doc.setReason(suggestion.getRationale());
                doc.setSuggestedBy("AI");
                doc.setStatus("PENDING_REVIEW"); // CRITICAL: NEVER auto-approve
                doc.setUpdatedAt(Instant.now());

                doc = aiMappingRepository.save(doc);

                job.setStatus("COMPLETED");
                job.setCompletedAt(Instant.now());
                job.setUpdatedAt(Instant.now());
                Map<String, Object> resultMap = new LinkedHashMap<>();
                resultMap.put("suggestionCount", 1);
                resultMap.put("canonicalField", doc.getCanonicalField());
                if (doc.getConfidence() != null) {
                    resultMap.put("confidence", doc.getConfidence());
                }
                job.setResult(resultMap);
                aiJobRepository.save(job);

                return doc;
            } else {
                job.setStatus("COMPLETED");
                job.setCompletedAt(Instant.now());
                job.setUpdatedAt(Instant.now());
                job.setResult(Map.of("suggestionCount", 0));
                aiJobRepository.save(job);
                return doc;
            }
        } catch (Exception ex) {
            log.error("AI suggestion provider failed for unknown syntax [{}]: {}", doc.getRawSyntax(), ex.getMessage());
            job.setStatus("FAILED");
            job.setError(ex.getMessage());
            job.setCompletedAt(Instant.now());
            job.setUpdatedAt(Instant.now());
            aiJobRepository.save(job);
            throw ex;
        }
    }

    @Override
    public AiMappingDocument approve(String id, String reviewerId) {
        if (reviewerId == null || reviewerId.isBlank()) {
            throw new IllegalArgumentException("Reviewer ID must be non-blank for mapping approval");
        }

        AiMappingDocument doc = aiMappingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Unknown syntax mapping not found for ID: " + id));

        if ("REJECTED".equalsIgnoreCase(doc.getStatus())) {
            throw new IllegalStateException("Cannot approve a rejected mapping");
        }
        if ("APPROVED".equalsIgnoreCase(doc.getStatus())) {
            throw new IllegalStateException("Mapping is already approved");
        }
        if (!"PENDING_REVIEW".equalsIgnoreCase(doc.getStatus())) {
            throw new IllegalStateException("Cannot approve mapping with status: " + doc.getStatus());
        }

        if (doc.getCanonicalField() == null || doc.getMappedValue() == null) {
            throw new IllegalStateException("Cannot approve mapping without valid proposed suggestion");
        }

        // Validate before approval
        CanonicalFieldAllowlist.validateFieldAndValue(doc.getCanonicalField(), doc.getMappedValue());

        doc.setStatus("APPROVED");
        doc.setReview(new AiReview(reviewerId.trim(), Instant.now(), null));
        doc.setUpdatedAt(Instant.now());

        return aiMappingRepository.save(doc);
    }

    @Override
    public AiMappingDocument reject(String id, String reviewerId, String reason) {
        if (reviewerId == null || reviewerId.isBlank()) {
            throw new IllegalArgumentException("Reviewer ID must be non-blank for mapping rejection");
        }

        AiMappingDocument doc = aiMappingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Unknown syntax mapping not found for ID: " + id));

        doc.setStatus("REJECTED");
        doc.setReview(new AiReview(reviewerId.trim(), Instant.now(), reason));
        doc.setUpdatedAt(Instant.now());

        return aiMappingRepository.save(doc);
    }

    @Override
    public void incrementUsage(String mappingId) {
        if (mappingId == null || mappingId.isBlank()) {
            return;
        }
        aiMappingRepository.findById(mappingId).ifPresent(doc -> {
            doc.setUsageCount(doc.getUsageCount() + 1);
            doc.setUpdatedAt(Instant.now());
            aiMappingRepository.save(doc);
        });
    }

    @Override
    public Optional<AiMappingDocument> getById(String id) {
        return aiMappingRepository.findById(id);
    }

    @Override
    public List<AiMappingDocument> getPendingReview() {
        return aiMappingRepository.findByStatus("PENDING_REVIEW");
    }

    @Override
    public List<AiMappingDocument> getApproved(String vendor, String platform) {
        return aiMappingRepository.findByVendorAndPlatformAndStatus(vendor, platform, "APPROVED");
    }
}
