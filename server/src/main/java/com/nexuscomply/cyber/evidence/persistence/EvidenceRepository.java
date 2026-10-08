package com.nexuscomply.cyber.evidence.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for EvidenceDocument.
 * Indexes per schema1.md section 11: findingId, auditId, configurationId, versionId.
 */
@Repository("cyberEvidenceRepository")
public interface EvidenceRepository extends MongoRepository<EvidenceDocument, String> {

    List<EvidenceDocument> findByFindingId(String findingId);

    List<EvidenceDocument> findByAuditId(String auditId);

    List<EvidenceDocument> findByConfigurationId(String configurationId);

    List<EvidenceDocument> findByVersionId(String versionId);

    Optional<EvidenceDocument> findFirstByFindingId(String findingId);
}
