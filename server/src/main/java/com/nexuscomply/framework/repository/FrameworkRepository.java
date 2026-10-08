package com.nexuscomply.framework.repository;

import com.nexuscomply.framework.model.Framework;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FrameworkRepository extends MongoRepository<Framework, String> {
    Optional<Framework> findByCode(String code);
}
