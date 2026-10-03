package com.nexuscomply.cyber.remediation.service;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemediationVerificationResult {

    private final String findingId;
    private final String canonicalField;
    private final Object expectedValue;
    private final Object originalActualValue;
    private final Object currentActualValue;
    private final boolean resolved;
    private final String message;
    private final Instant verifiedAt;

    public RemediationVerificationResult(
            String findingId,
            String canonicalField,
            Object expectedValue,
            Object originalActualValue,
            Object currentActualValue,
            boolean resolved,
            String message,
            Instant verifiedAt) {
        this.findingId = findingId;
        this.canonicalField = canonicalField;
        this.expectedValue = expectedValue;
        this.originalActualValue = originalActualValue;
        this.currentActualValue = currentActualValue;
        this.resolved = resolved;
        this.message = message;
        this.verifiedAt = verifiedAt;
    }

    public String getFindingId() {
        return findingId;
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    public Object getOriginalActualValue() {
        return originalActualValue;
    }

    public Object getCurrentActualValue() {
        return currentActualValue;
    }

    public boolean isResolved() {
        return resolved;
    }

    public String getMessage() {
        return message;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }
}
