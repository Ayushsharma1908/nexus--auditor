package com.nexuscomply.risk.repository;

import com.nexuscomply.risk.model.RiskAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskAssessmentRepository extends MongoRepository<RiskAssessment, String> {
    Page<RiskAssessment> findAllByDeviceIdNotNull(Pageable pageable);
    Page<RiskAssessment> findAllByFindingIdNotNull(Pageable pageable);
    Optional<RiskAssessment> findFirstByDeviceId(String deviceId);
    Optional<RiskAssessment> findFirstByFindingId(String findingId);
    List<RiskAssessment> findAllByOrderByCalculatedAtDesc(Pageable pageable);
    Page<RiskAssessment> findAllByAuditId(String auditId, Pageable pageable);
}
