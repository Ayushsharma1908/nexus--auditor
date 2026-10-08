package com.nexuscomply.remediation.repository;

import com.nexuscomply.remediation.model.RemediationTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RemediationTemplateRepository extends MongoRepository<RemediationTemplate, String> {
}
