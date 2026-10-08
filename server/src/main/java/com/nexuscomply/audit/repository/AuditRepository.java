package com.nexuscomply.audit.repository;

import com.nexuscomply.audit.model.Audit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRepository extends MongoRepository<Audit, String> {
    List<Audit> findAllByOrderByStartedAtDesc();
    Page<Audit> findAllByOrderByStartedAtDesc(Pageable pageable);
}
