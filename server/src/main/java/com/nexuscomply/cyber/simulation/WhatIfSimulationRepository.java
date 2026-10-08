package com.nexuscomply.cyber.simulation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("cyberWhatIfSimulationRepository")
public interface WhatIfSimulationRepository extends MongoRepository<WhatIfSimulationDocument, String> {
    List<WhatIfSimulationDocument> findByDeviceId(String deviceId);
    List<WhatIfSimulationDocument> findByBaseConfigurationVersionId(String baseConfigurationVersionId);
}
