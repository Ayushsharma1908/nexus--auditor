package com.nexuscomply.cyber.drift.service;

import com.nexuscomply.cyber.drift.model.DriftEvent;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;

import java.util.List;
import java.util.Optional;

/**
 * Service for detecting, classifying, and calculating risk deltas for configuration drift
 * between two versions of a device's normalized configuration.
 */
public interface DriftDetectionService {

    /**
     * Compares two NormalizedConfigurationDocuments for the same deviceId,
     * filters cosmetic noise, classifies each change (IMPROVED, DEGRADED, NO_SECURITY_IMPACT, UNKNOWN_IMPACT),
     * computes the risk delta using RiskCalculationService, and persists the DriftEvent.
     *
     * Conforms to Absolute Rule 6: read-only comparison, neither input document is ever mutated.
     */
    DriftEvent detectDrift(NormalizedConfigurationDocument beforeDoc, NormalizedConfigurationDocument afterDoc);

    /**
     * Looks up configurations by deviceId and version IDs and executes drift detection.
     */
    DriftEvent detectDriftByVersionIds(String deviceId, String fromVersionId, String toVersionId);

    /**
     * Looks up configurations by document IDs and executes drift detection.
     */
    DriftEvent detectDriftByDocumentIds(String fromDocId, String toDocId);

    /**
     * Retrieves a drift event by ID.
     */
    Optional<DriftEvent> getDriftEventById(String id);

    /**
     * Retrieves all drift events for a device.
     */
    List<DriftEvent> getDriftEventsByDeviceId(String deviceId);

    /**
     * Calculates overall drift impact according to risk delta and unknown status.
     */
    String calculateImpact(List<com.nexuscomply.cyber.drift.model.DriftChange> changes, int totalRiskBefore, int totalRiskAfter);
}
