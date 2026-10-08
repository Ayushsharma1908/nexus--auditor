package com.nexuscomply.remediation.repository;

import com.nexuscomply.remediation.model.RemediationPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RemediationPlanRepository extends MongoRepository<RemediationPlan, String> {
    Optional<RemediationPlan> findFirstByFindingId(String findingId);
}
