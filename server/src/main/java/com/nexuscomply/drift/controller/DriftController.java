package com.nexuscomply.drift.controller;

import com.nexuscomply.drift.dto.DriftCompareRequest;
import com.nexuscomply.drift.dto.DriftDTO;
import com.nexuscomply.drift.service.DriftService;

import com.nexuscomply.common.api.ApiResponse;
import com.nexuscomply.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * BUG-006, BUG-007, BUG-008, BUG-009, BUG-015, BUG-018 fixed.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Drift", description = "Configuration drift endpoints (B-053 → B-058)")
public class DriftController {

    private final DriftService driftService;

    public DriftController(DriftService driftService) {
        this.driftService = driftService;
    }

    // B-053 GET /api/v1/drift
    @GetMapping("/drift")
    @Operation(summary = "List drift events (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<DriftDTO>>> listDriftEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(driftService.listDriftEvents(pageable))));
    }

    // B-054 GET /api/v1/drift/{id}
    @GetMapping("/drift/{id}")
    @Operation(summary = "Get a specific drift event")
    public ResponseEntity<ApiResponse<DriftDTO>> getDriftEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(driftService.getDriftEvent(id)));
    }

    // B-055 GET /api/v1/devices/{id}/drift
    @GetMapping("/devices/{id}/drift")
    @Operation(summary = "List drift events for a specific device (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<DriftDTO>>> getDeviceDrift(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(driftService.getDeviceDrift(id, pageable))));
    }

    // B-056 POST /api/v1/drift/compare — BUG-009 fixed
    @PostMapping("/drift/compare")
    @Operation(summary = "Compare two configuration versions and detect drift")
    public ResponseEntity<ApiResponse<DriftDTO>> compareVersions(
            @Valid @RequestBody DriftCompareRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(driftService.compareVersions(request)));
    }

    // B-057 GET /api/v1/drift/{id}/affected-controls
    @GetMapping("/drift/{id}/affected-controls")
    @Operation(summary = "Get controls affected by a drift event")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAffectedControls(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(driftService.getAffectedControls(id)));
    }

    // B-058 GET /api/v1/drift/{id}/risk-impact
    @GetMapping("/drift/{id}/risk-impact")
    @Operation(summary = "Get risk impact of a drift event")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriftRiskImpact(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(driftService.getDriftRiskImpact(id)));
    }
}
