package com.nexuscomply.device.service;

import com.nexuscomply.common.exception.ResourceNotFoundException;
import com.nexuscomply.device.model.Device;
import com.nexuscomply.device.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    public Device getDeviceById(String id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device with ID '" + id + "' was not found."));
    }

    public Device saveDevice(Device device) {
        return deviceRepository.save(device);
    }

    public void deleteDevice(String id) {
        deviceRepository.deleteById(id);
    }
}

