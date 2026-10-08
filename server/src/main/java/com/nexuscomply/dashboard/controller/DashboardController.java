package com.nexuscomply.dashboard.controller;

import com.nexuscomply.dashboard.service.DashboardService;

import com.nexuscomply.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * BUG-005, BUG-012, BUG-018 fixed: DashboardController now uses ApiResponse wrapper
 * and DashboardService returns real aggregate data.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Dashboard aggregate endpoints (B-080 → B-086)")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // B-080 GET /api/v1/dashboard/summary
    @GetMapping("/summary")
    @Operation(summary = "Overall platform summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary() {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.getSummary()));
    }

    // B-081 GET /api/v1/dashboard/compliance
    @GetMapping("/compliance")
    @Operation(summary = "Compliance metrics summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCompliance() {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.getCompliance()));
    }

    // B-082 GET /api/v1/dashboard/findings
    @GetMapping("/findings")
    @Operation(summary = "Finding statistics summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFindings() {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.getFindings()));
    }

    // B-083 GET /api/v1/dashboard/risk
    @GetMapping("/risk")
    @Operation(summary = "Risk overview summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRisk() {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.getRisk()));
    }

    // B-084 GET /api/v1/dashboard/drift
    @GetMapping("/drift")
    @Operation(summary = "Drift activity summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDrift() {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.getDrift()));
    }

    // B-085 GET /api/v1/dashboard/activity
    @GetMapping("/activity")
    @Operation(summary = "Recent activity feed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActivity() {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.getActivity()));
    }

    // B-086 GET /api/v1/dashboard/frameworks
    @GetMapping("/frameworks")
    @Operation(summary = "Framework progress summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFrameworks() {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.getFrameworks()));
    }
}
