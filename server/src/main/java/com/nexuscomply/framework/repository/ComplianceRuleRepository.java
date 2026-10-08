package com.nexuscomply.framework.repository;

import com.nexuscomply.framework.model.ComplianceRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplianceRuleRepository extends MongoRepository<ComplianceRule, String> {
    List<ComplianceRule> findByControlId(String controlId);
    List<ComplianceRule> findByStatus(String status);
    List<ComplianceRule> findByTargetPath(String targetPath);
}
