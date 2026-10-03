package com.nexuscomply.cyber.remediation.model;

/**
 * Indicates whether a remediation command is directly derivable from parser regex
 * or is a representative example requiring operational elaboration, or represents
 * a platform capability gap.
 */
public enum CommandType {
    DERIVABLE_REGEX,
    REPRESENTATIVE_EXAMPLE,
    PLATFORM_GAP
}
