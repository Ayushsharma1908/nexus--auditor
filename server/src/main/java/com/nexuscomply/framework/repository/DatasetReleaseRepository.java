package com.nexuscomply.framework.repository;

import com.nexuscomply.framework.model.DatasetRelease;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatasetReleaseRepository extends MongoRepository<DatasetRelease, String> {
    Optional<DatasetRelease> findByDatasetVersion(String datasetVersion);
    Optional<DatasetRelease> findTopByOrderByReleaseDateDesc();
}
