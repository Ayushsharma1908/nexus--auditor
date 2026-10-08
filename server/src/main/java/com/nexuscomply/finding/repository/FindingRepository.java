package com.nexuscomply.finding.repository;

import com.nexuscomply.finding.model.Finding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FindingRepository extends MongoRepository<Finding, String> {
    List<Finding> findByAuditId(String auditId);
    List<Finding> findByDeviceId(String deviceId);
    List<Finding> findByControlId(String controlId);
    List<Finding> findByRuleId(String ruleId);
    List<Finding> findBySeverity(String severity);
    List<Finding> findByStatus(String status);
    Page<Finding> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
