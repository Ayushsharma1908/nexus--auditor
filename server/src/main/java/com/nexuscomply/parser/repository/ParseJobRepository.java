package com.nexuscomply.parser.repository;

import com.nexuscomply.parser.model.ParseJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParseJobRepository extends MongoRepository<ParseJob, String> {
    Optional<ParseJob> findFirstByConfigurationId(String configurationId);
    Optional<ParseJob> findFirstByVersionId(String versionId);
}
