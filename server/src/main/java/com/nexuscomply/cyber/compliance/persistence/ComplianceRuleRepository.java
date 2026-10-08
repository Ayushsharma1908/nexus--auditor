package com.nexuscomply.cyber.compliance.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("cyberComplianceRuleRepository")
public interface ComplianceRuleRepository extends MongoRepository<ComplianceRuleDocument, String> {

    Optional<ComplianceRuleDocument> findByRuleCode(String ruleCode);

    List<ComplianceRuleDocument> findByControlId(String controlId);

    List<ComplianceRuleDocument> findByFrameworkIdsContaining(String frameworkId);

    List<ComplianceRuleDocument> findByStatus(String status);
}
