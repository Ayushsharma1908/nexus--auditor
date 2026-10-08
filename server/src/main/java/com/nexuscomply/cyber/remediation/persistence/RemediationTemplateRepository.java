package com.nexuscomply.cyber.remediation.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("cyberRemediationTemplateRepository")
public interface RemediationTemplateRepository extends MongoRepository<RemediationTemplateDocument, String> {

    Optional<RemediationTemplateDocument> findByVendorAndPlatformAndCanonicalField(String vendor, String platform, String canonicalField);

    List<RemediationTemplateDocument> findByCanonicalField(String canonicalField);

    List<RemediationTemplateDocument> findByVendorAndPlatform(String vendor, String platform);

    List<RemediationTemplateDocument> findByControlId(String controlId);

    List<RemediationTemplateDocument> findByRuleId(String ruleId);
}
