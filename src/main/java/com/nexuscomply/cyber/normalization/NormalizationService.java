package com.nexuscomply.cyber.normalization;

import java.util.Optional;

public interface NormalizationService {

    NormalizedConfigurationDocument normalizeAndPersist(
            String rawConfig,
            String deviceId,
            String configurationId,
            String versionId,
            String vendor,
            String platform,
            String osVersion
    );

    Optional<NormalizedConfigurationDocument> getById(String id);

    Optional<NormalizedConfigurationDocument> getByVersionId(String versionId);
}
