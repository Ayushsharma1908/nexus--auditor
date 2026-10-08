package com.nexuscomply.framework.repository;

import com.nexuscomply.framework.model.EvaluationCase;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationCaseRepository extends MongoRepository<EvaluationCase, String> {
    List<EvaluationCase> findByVendor(String vendor);
    List<EvaluationCase> findByVendorAndPlatform(String vendor, String platform);
    List<EvaluationCase> findByExpectedResult(String expectedResult);
}
