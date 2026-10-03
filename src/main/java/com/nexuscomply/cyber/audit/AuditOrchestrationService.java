package com.nexuscomply.cyber.audit;

import java.util.Optional;

/**
 * Service orchestrating the complete compliance audit pipeline.
 *
 * <p>Pipeline stages:
 * QUEUED -> DETECTING -> PARSING -> NORMALIZING -> UNKNOWN_REVIEW -> CHECKING -> RISK_CALCULATION -> COMPLETED
 * Terminal non-happy path: FAILED, CANCELLED.
 */
public interface AuditOrchestrationService {

    /**
     * Executes the full audit pipeline for a given device configuration.
     *
     * @param deviceId the device identifier
     * @param configurationId the configuration identifier
     * @param rawConfig raw configuration text
     * @return the resulting Audit domain object
     */
    Audit startAudit(String deviceId, String configurationId, String rawConfig);

    /**
     * Executes the full audit pipeline with explicit versionId.
     *
     * @param deviceId the device identifier
     * @param configurationId the configuration identifier
     * @param versionId the configuration version identifier
     * @param rawConfig raw configuration text
     * @return the resulting Audit domain object
     */
    Audit startAudit(String deviceId, String configurationId, String versionId, String rawConfig);

    /**
     * Executes the full audit pipeline with explicit versionId and contextual risk parameters.
     *
     * @param deviceId the device identifier
     * @param configurationId the configuration identifier
     * @param versionId the configuration version identifier
     * @param rawConfig raw configuration text
     * @param riskContext contextual risk inputs (asset criticality, network exposure)
     * @return the resulting Audit domain object
     */
    Audit startAudit(String deviceId, String configurationId, String versionId, String rawConfig, com.nexuscomply.cyber.risk.RiskContext riskContext);

    /**
     * Cancels an ongoing audit execution.
     *
     * @param auditId the audit ID
     * @return true if cancelled successfully
     * @throws IllegalStateException if audit is already in a terminal state (COMPLETED, FAILED)
     */
    boolean cancel(String auditId);

    /**
     * Retrieves the current audit record by ID.
     *
     * @param auditId the audit ID
     * @return Optional containing Audit if found
     */
    Optional<Audit> getAudit(String auditId);
}
