package com.nexuscomply.cyber.drift.model;

/**
 * Classification of a canonical configuration change relative to compliance requirements.
 */
public enum DriftClassification {
    IMPROVED,
    DEGRADED,
    NO_SECURITY_IMPACT,
    UNKNOWN_IMPACT
}
