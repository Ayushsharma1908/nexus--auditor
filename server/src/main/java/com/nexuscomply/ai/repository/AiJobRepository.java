package com.nexuscomply.ai.repository;

import com.nexuscomply.ai.model.AiJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiJobRepository extends MongoRepository<AiJob, String> {
}
