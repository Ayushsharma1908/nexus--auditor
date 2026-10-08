package com.nexuscomply.risk.controller;

import com.nexuscomply.risk.dto.RiskDTO;
import com.nexuscomply.risk.service.RiskService;

import com.nexuscomply.common.api.ApiResponse;
import com.nexuscomply.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * BUG-006, BUG-008, BUG-013, BUG-015, BUG-018 fixed.
 */
@RestController
@RequestMapping("/api/v1/risk")
@Tag(name = "Risk", description = "Risk assessment endpoints (B-046 → B-052)")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    // B-046 GET /api/v1/risk
    @GetMapping
    @Operation(summary = "List all risk assessments (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<RiskDTO>>> listRisks(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(riskService.listRisks(pageable))));
    }

    // B-047 GET /api/v1/risk/findings
    @GetMapping("/findings")
    @Operation(summary = "List risk assessments linked to findings")
    public ResponseEntity<ApiResponse<PageResponse<RiskDTO>>> listRisksByFindings(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(riskService.listRisksByFindings(pageable))));
    }

    // B-048 GET /api/v1/risk/devices
    @GetMapping("/devices")
    @Operation(summary = "List risk assessments linked to devices")
    public ResponseEntity<ApiResponse<PageResponse<RiskDTO>>> listRisksByDevices(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(riskService.listRisksByDevices(pageable))));
    }

    // B-049 GET /api/v1/risk/devices/{id}
    @GetMapping("/devices/{id}")
    @Operation(summary = "Get risk assessment for a specific device")
    public ResponseEntity<ApiResponse<RiskDTO>> getRiskForDevice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(riskService.getRiskForDevice(id)));
    }

    // B-050 GET /api/v1/risk/trend
    @GetMapping("/trend")
    @Operation(summary = "Get risk trend over time (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<RiskDTO>>> getRiskTrend(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(riskService.getRiskTrend(pageable))));
    }

    // B-051 POST /api/v1/risk/recalculate/{auditId}
    @PostMapping("/recalculate/{auditId}")
    @Operation(summary = "Trigger async risk recalculation for an audit")
    public ResponseEntity<ApiResponse<Void>> recalculateRisk(@PathVariable UUID auditId) {
        riskService.recalculateRisk(auditId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.of(null));
    }

    // B-052 GET /api/v1/risk/{findingId}
    @GetMapping("/{findingId}")
    @Operation(summary = "Get risk assessment for a specific finding")
    public ResponseEntity<ApiResponse<RiskDTO>> getRiskForFinding(@PathVariable UUID findingId) {
        return ResponseEntity.ok(ApiResponse.of(riskService.getRiskForFinding(findingId)));
    }
}
