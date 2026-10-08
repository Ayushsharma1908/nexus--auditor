package com.nexuscomply.cyber.finding.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for findings collection.
 */
@Repository("cyberFindingRepository")
public interface FindingRepository extends MongoRepository<FindingDocument, String> {

    List<FindingDocument> findByAuditId(String auditId);

    List<FindingDocument> findByDeviceId(String deviceId);

    List<FindingDocument> findByStatus(String status);

    List<FindingDocument> findByAuditIdAndStatus(String auditId, String status);

    List<FindingDocument> findByDeviceIdAndStatus(String deviceId, String status);

    List<FindingDocument> findByRuleId(String ruleId);

    List<FindingDocument> findByControlId(String controlId);
}
