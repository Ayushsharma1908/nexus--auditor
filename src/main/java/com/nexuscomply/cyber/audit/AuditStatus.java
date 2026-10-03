package com.nexuscomply.cyber.audit;

/**
 * 10-state lifecycle state machine for audit execution.
 * Pre-resolved design decision 1:
 * Happy path: QUEUED -> DETECTING -> PARSING -> NORMALIZING -> UNKNOWN_REVIEW -> CHECKING -> RISK_CALCULATION -> COMPLETED
 * Terminal non-happy path: FAILED, CANCELLED
 */
public enum AuditStatus {
    QUEUED,
    DETECTING,
    PARSING,
    NORMALIZING,
    UNKNOWN_REVIEW,
    CHECKING,
    RISK_CALCULATION,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
