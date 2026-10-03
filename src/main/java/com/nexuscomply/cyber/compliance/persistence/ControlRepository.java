package com.nexuscomply.cyber.compliance.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ControlRepository extends MongoRepository<ControlDocument, String> {

    List<ControlDocument> findByFrameworkId(String frameworkId);

    Optional<ControlDocument> findByFrameworkIdAndControlId(String frameworkId, String controlId);

    List<ControlDocument> findByStatus(String status);
}
