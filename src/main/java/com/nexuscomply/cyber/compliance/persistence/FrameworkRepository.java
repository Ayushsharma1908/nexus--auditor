package com.nexuscomply.cyber.compliance.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FrameworkRepository extends MongoRepository<FrameworkDocument, String> {

    Optional<FrameworkDocument> findByCode(String code);

    Optional<FrameworkDocument> findByCodeAndVersion(String code, String version);

    List<FrameworkDocument> findByStatus(String status);
}
