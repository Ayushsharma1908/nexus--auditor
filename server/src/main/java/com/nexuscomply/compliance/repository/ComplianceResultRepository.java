package com.nexuscomply.compliance.repository;

import com.nexuscomply.compliance.model.ComplianceResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplianceResultRepository extends MongoRepository<ComplianceResult, String> {
    List<ComplianceResult> findByAuditId(String auditId);
    List<ComplianceResult> findByAuditIdAndFrameworkId(String auditId, String frameworkId);
    List<ComplianceResult> findByAuditIdAndControlId(String auditId, String controlId);
}
