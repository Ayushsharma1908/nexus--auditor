package com.nexuscomply.cyber.normalization;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NormalizedConfigurationRepository extends MongoRepository<NormalizedConfigurationDocument, String> {

    Optional<NormalizedConfigurationDocument> findByVersionId(String versionId);

    List<NormalizedConfigurationDocument> findByDeviceId(String deviceId);

    Optional<NormalizedConfigurationDocument> findByConfigurationId(String configurationId);
}
