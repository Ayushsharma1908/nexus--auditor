package com.nexuscomply.normalization.repository;

import com.nexuscomply.normalization.model.NormalizedConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NormalizedConfigurationRepository extends MongoRepository<NormalizedConfiguration, String> {
    Optional<NormalizedConfiguration> findFirstByVersionId(String versionId);
    Optional<NormalizedConfiguration> findFirstByConfigurationId(String configurationId);
    Optional<NormalizedConfiguration> findFirstByDeviceId(String deviceId);
}
