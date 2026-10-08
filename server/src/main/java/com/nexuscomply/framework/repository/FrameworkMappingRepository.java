package com.nexuscomply.framework.repository;

import com.nexuscomply.framework.model.FrameworkMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FrameworkMappingRepository extends MongoRepository<FrameworkMapping, String> {
    List<FrameworkMapping> findBySourceFrameworkId(String sourceFrameworkId);
    List<FrameworkMapping> findBySourceControlId(String sourceControlId);
    List<FrameworkMapping> findByTargetControlId(String targetControlId);
}
