package com.nexuscomply.cyber.finding;

import com.nexuscomply.cyber.canonical.SourceMapEntry;

import java.util.List;

/**
 * Contextual pipeline metadata passed to finding creation.
 */
public record FindingContext(
        String auditId,
        String deviceId,
        String configurationId,
        String versionId,
        List<SourceMapEntry> sourceMap
) {
    public FindingContext(String auditId, String deviceId, String configurationId, String versionId) {
        this(auditId, deviceId, configurationId, versionId, List.of());
    }

    public FindingContext(String auditId, String deviceId, String configurationId) {
        this(auditId, deviceId, configurationId, null, List.of());
    }
}
