package com.nexuscomply.framework.repository;

import com.nexuscomply.framework.model.Control;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ControlRepository extends MongoRepository<Control, String> {
    List<Control> findByFrameworkId(String frameworkId);
    Optional<Control> findByFrameworkIdAndControlCode(String frameworkId, String controlCode);
}
