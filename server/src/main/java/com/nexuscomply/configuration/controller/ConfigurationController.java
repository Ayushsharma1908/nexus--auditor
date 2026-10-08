package com.nexuscomply.configuration.controller;

import com.nexuscomply.common.api.ApiResponse;
import com.nexuscomply.common.security.RequestIdFilter;
import com.nexuscomply.configuration.model.Configuration;
import com.nexuscomply.configuration.service.ConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/configurations")
@Tag(name = "Configurations", description = "Device configuration versions and syntax inspection")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    @Operation(summary = "List configurations", description = "Retrieves configuration versions optionally filtered by deviceId.")
    public ResponseEntity<ApiResponse<List<Configuration>>> listConfigurations(
            @RequestParam(name = "deviceId", required = false) String deviceId) {
        List<Configuration> configs = configurationService.getConfigurations(deviceId);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(configs, requestId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get configuration by ID", description = "Retrieves configuration text and metadata by ID.")
    public ResponseEntity<ApiResponse<Configuration>> getConfiguration(@PathVariable("id") String id) {
        Configuration config = configurationService.getConfigurationById(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(config, requestId));
    }

    @PostMapping
    @Operation(summary = "Upload or save configuration", description = "Stores a new configuration revision.")
    public ResponseEntity<ApiResponse<Configuration>> createConfiguration(@RequestBody Configuration config) {
        if (config.getId() == null || config.getId().isBlank()) {
            config.setId("cfg-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        }
        if (config.getUploadedAt() == null || config.getUploadedAt().isBlank()) {
            config.setUploadedAt(java.time.Instant.now().toString());
        }
        if (config.getHash() == null || config.getHash().isBlank()) {
            config.setHash("sha256:" + Integer.toHexString((config.getFilename() + System.currentTimeMillis()).hashCode()));
        }
        Configuration saved = configurationService.saveConfiguration(config);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(ApiResponse.of(saved, requestId));
    }
}

