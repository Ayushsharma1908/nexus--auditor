package com.nexuscomply.cyber.drift.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("cyberDriftEventRepository")
public interface DriftEventRepository extends MongoRepository<DriftEventDocument, String> {

    List<DriftEventDocument> findByDeviceId(String deviceId);

    List<DriftEventDocument> findByDeviceIdOrderByDetectedAtDesc(String deviceId);

    Optional<DriftEventDocument> findByDeviceIdAndFromVersionIdAndToVersionId(String deviceId, String fromVersionId, String toVersionId);

    List<DriftEventDocument> findByImpact(String impact);
}
