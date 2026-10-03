package com.nexuscomply.cyber.remediation.service;

import com.nexuscomply.cyber.finding.Finding;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;

/**
 * Read-only verification service that compares a Finding's original expected and actual
 * canonical values against a later NormalizedConfigurationDocument's canonical facts.
 * Conforms strictly to Absolute Rule 1 (no autonomous execution) and Absolute Rule 7 (read-only comparison).
 */
public interface RemediationVerificationService {

    /**
     * Determines whether a Finding appears resolved in a later normalized configuration.
     *
     * @param finding the original finding with violating canonical values
     * @param laterNormalizedDoc the later normalized configuration document (e.g. from post-remediation audit)
     * @return verification result detailing resolution status and values
     */
    RemediationVerificationResult verifyFinding(Finding finding, NormalizedConfigurationDocument laterNormalizedDoc);
}
