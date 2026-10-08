package com.nexuscomply.finding.controller;

import com.nexuscomply.common.api.ApiResponse;
import com.nexuscomply.common.security.RequestIdFilter;
import com.nexuscomply.finding.dto.UpdateSeverityRequest;
import com.nexuscomply.finding.dto.UpdateStatusRequest;
import com.nexuscomply.finding.model.Evidence;
import com.nexuscomply.finding.model.Finding;
import com.nexuscomply.finding.model.FindingHistoryEntry;
import com.nexuscomply.finding.service.FindingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/findings")
@Tag(name = "Findings", description = "Finding discovery, status lifecycle, history, and evidence links")
public class FindingController {

    private final FindingService findingService;

    public FindingController(FindingService findingService) {
        this.findingService = findingService;
    }

    /**
     * B-034: GET /api/v1/findings
     * List/filter findings.
     */
    @GetMapping
    @Operation(summary = "List/filter findings", description = "Retrieves findings filtered by auditId, severity, status, or deviceId.")
    public ResponseEntity<ApiResponse<List<Finding>>> listFindings(
            @RequestParam(name = "auditId", required = false) String auditId,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "deviceId", required = false) String deviceId) {
        List<Finding> findings = findingService.filterFindings(auditId, severity, status, deviceId);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(findings, requestId));
    }

    /**
     * B-035: GET /api/v1/findings/{id}
     * Get finding.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get finding", description = "Retrieves finding details by ID.")
    public ResponseEntity<ApiResponse<Finding>> getFinding(@PathVariable("id") String id) {
        Finding finding = findingService.getFindingById(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(finding, requestId));
    }

    /**
     * B-036: PATCH /api/v1/findings/{id}/status
     * Update finding status.
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update finding status", description = "Updates finding lifecycle status (OPEN, ACKNOWLEDGED, RESOLVED, SUPPRESSED).")
    public ResponseEntity<ApiResponse<Finding>> updateStatus(
            @PathVariable("id") String id,
            @RequestBody(required = false) UpdateStatusRequest request) {
        Finding finding = findingService.updateFindingStatus(id, request);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(finding, requestId));
    }

    /**
     * B-037: PATCH /api/v1/findings/{id}/severity
     * Update finding severity.
     */
    @PatchMapping("/{id}/severity")
    @Operation(summary = "Update finding severity", description = "Overrides or updates finding severity rating.")
    public ResponseEntity<ApiResponse<Finding>> updateSeverity(
            @PathVariable("id") String id,
            @RequestBody(required = false) UpdateSeverityRequest request) {
        Finding finding = findingService.updateFindingSeverity(id, request);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(finding, requestId));
    }

    /**
     * B-038: GET /api/v1/findings/{id}/history
     * Get finding history.
     */
    @GetMapping("/{id}/history")
    @Operation(summary = "Get finding history", description = "Retrieves chronological audit trail of changes.")
    public ResponseEntity<ApiResponse<List<FindingHistoryEntry>>> getHistory(@PathVariable("id") String id) {
        List<FindingHistoryEntry> history = findingService.getFindingHistory(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(history, requestId));
    }

    /**
     * B-039: GET /api/v1/findings/{id}/related
     * Get related findings.
     */
    @GetMapping("/{id}/related")
    @Operation(summary = "Get related findings", description = "Retrieves other findings impacting the same device or rule.")
    public ResponseEntity<ApiResponse<List<Finding>>> getRelated(@PathVariable("id") String id) {
        List<Finding> related = findingService.getRelatedFindings(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(related, requestId));
    }

    /**
     * B-040: POST /api/v1/findings/{id}/acknowledge
     * Acknowledge finding.
     */
    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge finding", description = "Transitions finding to ACKNOWLEDGED state.")
    public ResponseEntity<ApiResponse<Finding>> acknowledge(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        Finding finding = findingService.acknowledgeFinding(id, note);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(finding, requestId));
    }

    /**
     * B-041: POST /api/v1/findings/{id}/resolve
     * Resolve finding.
     */
    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve finding", description = "Transitions finding to RESOLVED state.")
    public ResponseEntity<ApiResponse<Finding>> resolve(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        Finding finding = findingService.resolveFinding(id, note);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(finding, requestId));
    }

    /**
     * B-042: GET /api/v1/findings/{id}/evidence
     * List finding evidence.
     */
    @GetMapping("/{id}/evidence")
    @Operation(summary = "List finding evidence", description = "Retrieves all evidence records associated with this finding.")
    public ResponseEntity<ApiResponse<List<Evidence>>> getEvidence(@PathVariable("id") String id) {
        List<Evidence> evidence = findingService.getFindingEvidence(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(evidence, requestId));
    }
}
