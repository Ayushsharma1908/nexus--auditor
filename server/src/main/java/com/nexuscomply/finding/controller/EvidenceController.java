package com.nexuscomply.finding.controller;

import com.nexuscomply.common.api.ApiResponse;
import com.nexuscomply.common.security.RequestIdFilter;
import com.nexuscomply.finding.model.Evidence;
import com.nexuscomply.finding.service.FindingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/evidence")
@Tag(name = "Evidence", description = "Evidence retrieval, source mapping, and configuration traceability")
public class EvidenceController {

    private final FindingService findingService;

    public EvidenceController(FindingService findingService) {
        this.findingService = findingService;
    }

    /**
     * B-043: GET /api/v1/evidence/{id}
     * Get evidence.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get evidence", description = "Retrieves evidence details by ID.")
    public ResponseEntity<ApiResponse<Evidence>> getEvidence(@PathVariable("id") String id) {
        Evidence evidence = findingService.getEvidenceById(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(evidence, requestId));
    }

    /**
     * B-044: GET /api/v1/evidence/{id}/source
     * Get evidence source.
     */
    @GetMapping("/{id}/source")
    @Operation(summary = "Get evidence source", description = "Retrieves file path, line numbers, and snippet for an evidence record.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEvidenceSource(@PathVariable("id") String id) {
        Map<String, Object> source = findingService.getEvidenceSource(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(source, requestId));
    }

    /**
     * B-045: GET /api/v1/evidence/{id}/configuration
     * Get evidence configuration.
     */
    @GetMapping("/{id}/configuration")
    @Operation(summary = "Get evidence configuration", description = "Retrieves configuration version and device context for an evidence record.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEvidenceConfiguration(@PathVariable("id") String id) {
        Map<String, Object> config = findingService.getEvidenceConfiguration(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(config, requestId));
    }
}
