package com.nexuscomply.normalization.controller;

import com.nexuscomply.common.api.ApiResponse;
import com.nexuscomply.common.security.RequestIdFilter;
import com.nexuscomply.normalization.dto.NormalizeRequest;
import com.nexuscomply.normalization.dto.NormalizedConfigurationResponse;
import com.nexuscomply.normalization.dto.SourceMapResponse;
import com.nexuscomply.normalization.model.NormalizedConfiguration;
import com.nexuscomply.normalization.model.SourceMapEntry;
import com.nexuscomply.normalization.service.NormalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Normalization", description = "Normalization operations creating canonical security state")
public class NormalizationController {

    private final NormalizationService normalizationService;

    public NormalizationController(NormalizationService normalizationService) {
        this.normalizationService = normalizationService;
    }

    /**
     * B-005: POST /api/v1/cyber/normalize
     * Start normalization through NormalizationService.
     */
    @PostMapping("/cyber/normalize")
    @Operation(summary = "Start normalization", description = "Normalizes parsed configuration into canonical security state.")
    public ResponseEntity<ApiResponse<NormalizedConfigurationResponse>> normalize(@RequestBody(required = false) NormalizeRequest request) {
        NormalizedConfiguration config = normalizationService.normalizeConfiguration(request);
        NormalizedConfigurationResponse response = NormalizedConfigurationResponse.fromEntity(config);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response, requestId));
    }

    /**
     * B-006: GET /api/v1/normalized-configurations/{id}
     * Retrieve normalized configuration.
     */
    @GetMapping("/normalized-configurations/{id}")
    @Operation(summary = "Retrieve normalized configuration", description = "Gets canonical configuration state by ID.")
    public ResponseEntity<ApiResponse<NormalizedConfigurationResponse>> getNormalizedConfig(@PathVariable("id") String id) {
        NormalizedConfiguration config = normalizationService.getById(id);
        NormalizedConfigurationResponse response = NormalizedConfigurationResponse.fromEntity(config);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(response, requestId));
    }

    /**
     * B-007: GET /api/v1/configurations/versions/{id}/normalized
     * Retrieve normalized result for a configuration version.
     */
    @GetMapping("/configurations/versions/{id}/normalized")
    @Operation(summary = "Retrieve normalized result for a configuration version", description = "Gets canonical state for configuration version.")
    public ResponseEntity<ApiResponse<NormalizedConfigurationResponse>> getNormalizedConfigByVersion(@PathVariable("id") String id) {
        NormalizedConfiguration config = normalizationService.getByVersionId(id);
        NormalizedConfigurationResponse response = NormalizedConfigurationResponse.fromEntity(config);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(response, requestId));
    }

    /**
     * B-008: GET /api/v1/normalized-configurations/{id}/source-map
     * Retrieve source mapping.
     */
    @GetMapping("/normalized-configurations/{id}/source-map")
    @Operation(summary = "Retrieve source mapping", description = "Gets canonical fact to line source evidence mappings.")
    public ResponseEntity<ApiResponse<SourceMapResponse>> getSourceMap(@PathVariable("id") String id) {
        List<SourceMapEntry> entries = normalizationService.getSourceMap(id);
        NormalizedConfiguration config = normalizationService.getById(id);
        SourceMapResponse response = new SourceMapResponse(config.getId(), config.getConfigurationId(), entries);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(response, requestId));
    }
}
