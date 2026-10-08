package com.nexuscomply.cyber.remediation.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("cyberRemediationPlanRepository")
public interface RemediationPlanRepository extends MongoRepository<RemediationPlanDocument, String> {

    List<RemediationPlanDocument> findByFindingId(String findingId);

    List<RemediationPlanDocument> findByDeviceId(String deviceId);

    List<RemediationPlanDocument> findByTemplateId(String templateId);

    List<RemediationPlanDocument> findByStatus(String status);

    Optional<RemediationPlanDocument> findFirstByFindingIdOrderByCreatedAtDesc(String findingId);
}
