package com.nexuscomply.audit.controller;

import com.nexuscomply.audit.dto.AuditReportResponse;
import com.nexuscomply.audit.dto.AuditStatusResponse;
import com.nexuscomply.audit.dto.CreateAuditRequest;
import com.nexuscomply.audit.model.Audit;
import com.nexuscomply.audit.model.AuditRisk;
import com.nexuscomply.audit.model.AuditSummary;
import com.nexuscomply.audit.model.FrameworkResult;
import com.nexuscomply.audit.service.AuditService;
import com.nexuscomply.common.api.ApiResponse;
import com.nexuscomply.common.security.RequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audits")
@Tag(name = "Audits", description = "Audit lifecycle, evaluation, status, and reporting")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * B-017: POST /api/v1/audits
     * Create audit.
     */
    @PostMapping
    @Operation(summary = "Create audit", description = "Initiates a new compliance audit.")
    public ResponseEntity<ApiResponse<Audit>> createAudit(@RequestBody(required = false) CreateAuditRequest request) {
        Audit audit = auditService.createAudit(request);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(audit, requestId));
    }

    /**
     * B-018: GET /api/v1/audits
     * List audits.
     */
    @GetMapping
    @Operation(summary = "List audits", description = "Retrieves all compliance audits.")
    public ResponseEntity<ApiResponse<List<Audit>>> listAudits() {
        List<Audit> audits = auditService.getAllAudits();
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(audits, requestId));
    }

    /**
     * B-019: GET /api/v1/audits/{id}
     * Get audit.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get audit", description = "Retrieves audit details by ID.")
    public ResponseEntity<ApiResponse<Audit>> getAudit(@PathVariable("id") String id) {
        Audit audit = auditService.getAuditById(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(audit, requestId));
    }

    /**
     * B-020: GET /api/v1/audits/{id}/status
     * Get audit status.
     */
    @GetMapping("/{id}/status")
    @Operation(summary = "Get audit status", description = "Retrieves current execution status of an audit.")
    public ResponseEntity<ApiResponse<AuditStatusResponse>> getAuditStatus(@PathVariable("id") String id) {
        AuditStatusResponse status = auditService.getAuditStatus(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(status, requestId));
    }

    /**
     * B-021: POST /api/v1/audits/{id}/cancel
     * Cancel audit.
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel audit", description = "Cancels an in-progress or queued audit.")
    public ResponseEntity<ApiResponse<Audit>> cancelAudit(@PathVariable("id") String id) {
        Audit audit = auditService.cancelAudit(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(audit, requestId));
    }

    /**
     * B-022: POST /api/v1/audits/{id}/rerun
     * Rerun audit.
     */
    @PostMapping("/{id}/rerun")
    @Operation(summary = "Rerun audit", description = "Reruns an audit against updated device configurations.")
    public ResponseEntity<ApiResponse<Audit>> rerunAudit(@PathVariable("id") String id) {
        Audit audit = auditService.rerunAudit(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(audit, requestId));
    }

    /**
     * B-023: GET /api/v1/audits/{id}/summary
     * Get audit summary.
     */
    @GetMapping("/{id}/summary")
    @Operation(summary = "Get audit summary", description = "Retrieves statistical summary and pass/fail counts.")
    public ResponseEntity<ApiResponse<AuditSummary>> getAuditSummary(@PathVariable("id") String id) {
        AuditSummary summary = auditService.getAuditSummary(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(summary, requestId));
    }

    /**
     * B-024: GET /api/v1/audits/{id}/framework-results
     * Get framework results.
     */
    @GetMapping("/{id}/framework-results")
    @Operation(summary = "Get framework results", description = "Retrieves compliance score per evaluated framework.")
    public ResponseEntity<ApiResponse<List<FrameworkResult>>> getFrameworkResults(@PathVariable("id") String id) {
        List<FrameworkResult> results = auditService.getFrameworkResults(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(results, requestId));
    }

    /**
     * B-025: GET /api/v1/audits/{id}/findings
     * List audit findings.
     */
    @GetMapping("/{id}/findings")
    @Operation(summary = "List audit findings", description = "Retrieves all finding IDs generated by an audit.")
    public ResponseEntity<ApiResponse<List<String>>> getAuditFindings(@PathVariable("id") String id) {
        List<String> findings = auditService.getAuditFindings(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(findings, requestId));
    }

    /**
     * B-026: GET /api/v1/audits/{id}/risk
     * Get audit risk.
     */
    @GetMapping("/{id}/risk")
    @Operation(summary = "Get audit risk", description = "Retrieves risk score and severity breakdown.")
    public ResponseEntity<ApiResponse<AuditRisk>> getAuditRisk(@PathVariable("id") String id) {
        AuditRisk risk = auditService.getAuditRisk(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(risk, requestId));
    }

    /**
     * B-027: POST /api/v1/audits/{id}/report
     * Generate audit report.
     */
    @PostMapping("/{id}/report")
    @Operation(summary = "Generate audit report", description = "Generates a PDF or executive report from audit results.")
    public ResponseEntity<ApiResponse<AuditReportResponse>> generateReport(@PathVariable("id") String id) {
        AuditReportResponse report = auditService.generateReport(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(report, requestId));
    }
}
