package com.nexuscomply.finding.repository;

import com.nexuscomply.finding.model.Evidence;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceRepository extends MongoRepository<Evidence, String> {
    List<Evidence> findByFindingId(String findingId);
    List<Evidence> findByAuditId(String auditId);
    List<Evidence> findByDeviceId(String deviceId);
    List<Evidence> findByConfigurationId(String configurationId);
}
