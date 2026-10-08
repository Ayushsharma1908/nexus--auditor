package com.nexuscomply.cyber.ai.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("cyberAiJobRepository")
public interface AiJobRepository extends MongoRepository<AiJobDocument, String> {
    List<AiJobDocument> findByStatus(String status);
    List<AiJobDocument> findByType(String type);
}
