package com.nexuscomply.device.controller;

import com.nexuscomply.common.api.ApiResponse;
import com.nexuscomply.common.security.RequestIdFilter;
import com.nexuscomply.device.model.Device;
import com.nexuscomply.device.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Device inventory and posture")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    @Operation(summary = "List all devices", description = "Retrieves all network devices.")
    public ResponseEntity<ApiResponse<List<Device>>> listDevices() {
        List<Device> devices = deviceService.getAllDevices();
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(devices, requestId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get device by ID", description = "Retrieves a specific device by ID.")
    public ResponseEntity<ApiResponse<Device>> getDevice(@PathVariable("id") String id) {
        Device device = deviceService.getDeviceById(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(device, requestId));
    }

    @PostMapping
    @Operation(summary = "Create device", description = "Registers a new network device.")
    public ResponseEntity<ApiResponse<Device>> createDevice(@RequestBody Device device) {
        if (device.getId() == null || device.getId().isBlank()) {
            device.setId("dev-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        }
        if (device.getPlatform() == null) {
            device.setPlatform(device.getVendor() != null ? device.getVendor() + " OS" : "Generic OS");
        }
        if (device.getLastAudit() == null || device.getLastAudit().isBlank()) {
            device.setLastAudit(java.time.LocalDate.now().toString());
        }
        Device saved = deviceService.saveDevice(device);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(ApiResponse.of(saved, requestId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update device", description = "Updates an existing network device.")
    public ResponseEntity<ApiResponse<Device>> updateDevice(@PathVariable("id") String id, @RequestBody Device device) {
        device.setId(id);
        Device saved = deviceService.saveDevice(device);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(saved, requestId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete device", description = "Deletes a device from inventory.")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable("id") String id) {
        deviceService.deleteDevice(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(null, requestId));
    }
}

