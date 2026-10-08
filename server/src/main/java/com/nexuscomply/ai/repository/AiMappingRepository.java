package com.nexuscomply.ai.repository;

import com.nexuscomply.ai.model.AiMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiMappingRepository extends MongoRepository<AiMapping, String> {
}
