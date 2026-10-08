package com.nexuscomply.report.controller;

import com.nexuscomply.report.dto.CreateReportRequest;
import com.nexuscomply.report.dto.ReportDTO;
import com.nexuscomply.report.service.ReportService;

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
 * BUG-006, BUG-007, BUG-008, BUG-015, BUG-018 fixed.
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Report generation and download endpoints (B-087 → B-092)")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // B-087 POST /api/v1/reports — BUG-007, BUG-008 fixed
    @PostMapping
    @Operation(summary = "Create (request generation of) a new report")
    public ResponseEntity<ApiResponse<ReportDTO>> createReport(
            @Valid @RequestBody CreateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(reportService.createReport(request)));
    }

    // B-088 GET /api/v1/reports — BUG-006 fixed
    @GetMapping
    @Operation(summary = "List all reports (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<ReportDTO>>> listReports(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(reportService.listReports(pageable))));
    }

    // B-089 GET /api/v1/reports/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get a specific report")
    public ResponseEntity<ApiResponse<ReportDTO>> getReport(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(reportService.getReport(id)));
    }

    // B-090 GET /api/v1/reports/{id}/preview
    @GetMapping("/{id}/preview")
    @Operation(summary = "Preview a report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewReport(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(reportService.previewReport(id)));
    }

    // B-091 GET /api/v1/reports/{id}/download
    @GetMapping("/{id}/download")
    @Operation(summary = "Download a completed report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> downloadReport(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(reportService.downloadReport(id)));
    }

    // B-092 POST /api/v1/reports/{id}/regenerate
    @PostMapping("/{id}/regenerate")
    @Operation(summary = "Regenerate an existing report")
    public ResponseEntity<ApiResponse<ReportDTO>> regenerateReport(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(reportService.regenerateReport(id)));
    }
}
