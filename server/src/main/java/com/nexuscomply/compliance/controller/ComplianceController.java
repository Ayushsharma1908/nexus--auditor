package com.nexuscomply.compliance.controller;

import com.nexuscomply.common.api.ApiResponse;
import com.nexuscomply.common.security.RequestIdFilter;
import com.nexuscomply.compliance.dto.ComplianceSummaryResponse;
import com.nexuscomply.compliance.dto.EvaluateComplianceRequest;
import com.nexuscomply.compliance.model.ComplianceResult;
import com.nexuscomply.compliance.model.UnknownComplianceItem;
import com.nexuscomply.compliance.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/compliance")
@Tag(name = "Compliance", description = "Compliance rule evaluation and result diagnostics")
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    /**
     * B-028: POST /api/v1/compliance/evaluate
     * Evaluate compliance.
     */
    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate compliance", description = "Evaluates compliance rules against configurations.")
    public ResponseEntity<ApiResponse<List<ComplianceResult>>> evaluate(@RequestBody(required = false) EvaluateComplianceRequest request) {
        List<ComplianceResult> results = complianceService.evaluateCompliance(request);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(results, requestId));
    }

    /**
     * B-029: POST /api/v1/compliance/evaluate/control/{id}
     * Evaluate one control.
     */
    @PostMapping("/evaluate/control/{id}")
    @Operation(summary = "Evaluate one control", description = "Evaluates rules for a single control.")
    public ResponseEntity<ApiResponse<ComplianceResult>> evaluateControl(
            @PathVariable("id") String id,
            @RequestBody(required = false) EvaluateComplianceRequest request) {
        ComplianceResult result = complianceService.evaluateControl(id, request);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(result, requestId));
    }

    /**
     * B-030: POST /api/v1/compliance/evaluate/framework/{id}
     * Evaluate one framework.
     */
    @PostMapping("/evaluate/framework/{id}")
    @Operation(summary = "Evaluate one framework", description = "Evaluates all controls for a single framework.")
    public ResponseEntity<ApiResponse<List<ComplianceResult>>> evaluateFramework(
            @PathVariable("id") String id,
            @RequestBody(required = false) EvaluateComplianceRequest request) {
        List<ComplianceResult> results = complianceService.evaluateFramework(id, request);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(results, requestId));
    }

    /**
     * B-031: GET /api/v1/compliance/results/{auditId}
     * Get compliance results.
     */
    @GetMapping("/results/{auditId}")
    @Operation(summary = "Get compliance results", description = "Retrieves all control-level compliance results for an audit.")
    public ResponseEntity<ApiResponse<List<ComplianceResult>>> getResults(@PathVariable("auditId") String auditId) {
        List<ComplianceResult> results = complianceService.getResultsByAudit(auditId);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(results, requestId));
    }

    /**
     * B-032: GET /api/v1/compliance/results/{auditId}/summary
     * Get compliance summary.
     */
    @GetMapping("/results/{auditId}/summary")
    @Operation(summary = "Get compliance summary", description = "Retrieves summary metrics and pass rates for an audit.")
    public ResponseEntity<ApiResponse<ComplianceSummaryResponse>> getSummary(@PathVariable("auditId") String auditId) {
        ComplianceSummaryResponse summary = complianceService.getSummaryByAudit(auditId);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(summary, requestId));
    }

    /**
     * B-033: GET /api/v1/compliance/results/{auditId}/unknowns
     * Get unknown results.
     */
    @GetMapping("/results/{auditId}/unknowns")
    @Operation(summary = "Get unknown results", description = "Retrieves rules that could not be evaluated due to ambiguous or unknown configuration syntax.")
    public ResponseEntity<ApiResponse<List<UnknownComplianceItem>>> getUnknowns(@PathVariable("auditId") String auditId) {
        List<UnknownComplianceItem> unknowns = complianceService.getUnknownsByAudit(auditId);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(unknowns, requestId));
    }
}
