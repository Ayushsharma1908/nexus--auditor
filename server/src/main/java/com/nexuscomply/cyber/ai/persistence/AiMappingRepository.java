package com.nexuscomply.cyber.ai.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("cyberAiMappingRepository")
public interface AiMappingRepository extends MongoRepository<AiMappingDocument, String> {
    Optional<AiMappingDocument> findByVendorAndPlatformAndRawSyntax(String vendor, String platform, String rawSyntax);
    List<AiMappingDocument> findByVendorAndPlatformAndStatus(String vendor, String platform, String status);
    List<AiMappingDocument> findByStatus(String status);
}
