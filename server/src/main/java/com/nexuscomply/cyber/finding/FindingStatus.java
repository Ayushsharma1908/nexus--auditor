package com.nexuscomply.cyber.finding;

/**
 * Finding lifecycle statuses per schema1.md section 4.1.
 */
public enum FindingStatus {
    OPEN,
    ACKNOWLEDGED,
    IN_REVIEW,
    REMEDIATION_PLANNED,
    RESOLVED,
    FALSE_POSITIVE
}
