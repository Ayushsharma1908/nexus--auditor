package com.nexuscomply.framework.repository;

import com.nexuscomply.framework.model.VendorKnowledge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorKnowledgeRepository extends MongoRepository<VendorKnowledge, String> {
    List<VendorKnowledge> findByVendor(String vendor);
    List<VendorKnowledge> findByVendorAndPlatform(String vendor, String platform);
    List<VendorKnowledge> findByCanonicalField(String canonicalField);
}
