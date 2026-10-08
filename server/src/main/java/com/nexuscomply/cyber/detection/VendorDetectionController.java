package com.nexuscomply.cyber.detection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cyber")
public class VendorDetectionController {

    private final VendorDetectionService vendorDetectionService;

    public VendorDetectionController(VendorDetectionService vendorDetectionService) {
        this.vendorDetectionService = vendorDetectionService;
    }

    @PostMapping("/detect-vendor")
    public ResponseEntity<Map<String, Object>> detectVendor(@RequestBody VendorDetectionRequest request) {
        VendorDetectionResponse response = vendorDetectionService.detectVendor(request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", response);
        result.put("requestId", "REQ-" + UUID.randomUUID().toString().substring(0, 8));
        return ResponseEntity.ok(result);
    }
}
