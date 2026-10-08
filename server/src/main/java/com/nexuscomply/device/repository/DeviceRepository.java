package com.nexuscomply.device.repository;

import com.nexuscomply.device.model.Device;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends MongoRepository<Device, String> {
    List<Device> findByVendor(String vendor);
    List<Device> findByStatus(String status);
}
