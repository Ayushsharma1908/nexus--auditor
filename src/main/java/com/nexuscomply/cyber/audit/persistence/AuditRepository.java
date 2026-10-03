package com.nexuscomply.cyber.audit.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for AuditDocument.
 * Indexes per schema1.md section 9:
 * deviceId, configurationId, versionId, status, createdAt, (deviceId, createdAt).
 */
@Repository
public interface AuditRepository extends MongoRepository<AuditDocument, String> {

    List<AuditDocument> findByDeviceId(String deviceId);

    List<AuditDocument> findByConfigurationId(String configurationId);

    List<AuditDocument> findByVersionId(String versionId);

    List<AuditDocument> findByStatus(String status);
}
