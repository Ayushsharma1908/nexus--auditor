package com.nexuscomply.cyber.risk.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for risk_assessments collection.
 */
@Repository("cyberRiskAssessmentRepository")
public interface RiskAssessmentRepository extends MongoRepository<RiskAssessmentDocument, String> {

    List<RiskAssessmentDocument> findByAuditId(String auditId);

    List<RiskAssessmentDocument> findByDeviceId(String deviceId);

    Optional<RiskAssessmentDocument> findByFindingId(String findingId);

    List<RiskAssessmentDocument> findByLevel(String level);
}
