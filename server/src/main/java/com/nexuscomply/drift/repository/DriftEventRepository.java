package com.nexuscomply.drift.repository;

import com.nexuscomply.drift.model.DriftEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriftEventRepository extends MongoRepository<DriftEvent, String> {
    Page<DriftEvent> findByDeviceId(String deviceId, Pageable pageable);
    Page<DriftEvent> findAllByOrderByDetectedAtDesc(Pageable pageable);
}
