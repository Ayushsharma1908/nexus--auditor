package com.nexuscomply.remediation.controller;

import com.nexuscomply.remediation.dto.CreateRemediationPlanRequest;
import com.nexuscomply.remediation.dto.RemediationDTO;
import com.nexuscomply.remediation.service.RemediationService;

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
 * BUG-006, BUG-007, BUG-010, BUG-015, BUG-018 fixed.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Remediation", description = "Remediation template and plan endpoints (B-064 → B-070)")
public class RemediationController {

    private final RemediationService remediationService;

    public RemediationController(RemediationService remediationService) {
        this.remediationService = remediationService;
    }

    // B-064 GET /api/v1/remediation/templates — BUG-006 fixed
    @GetMapping("/remediation/templates")
    @Operation(summary = "List remediation templates (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<RemediationDTO>>> listTemplates(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(remediationService.listTemplates(pageable))));
    }

    // B-065 GET /api/v1/remediation/templates/{id}
    @GetMapping("/remediation/templates/{id}")
    @Operation(summary = "Get a specific remediation template")
    public ResponseEntity<ApiResponse<RemediationDTO>> getTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(remediationService.getTemplate(id)));
    }

    // B-066 GET /api/v1/findings/{id}/remediation
    @GetMapping("/findings/{id}/remediation")
    @Operation(summary = "Get remediation plan for a finding")
    public ResponseEntity<ApiResponse<RemediationDTO>> getRemediationForFinding(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(remediationService.getRemediationForFinding(id)));
    }

    // B-067 POST /api/v1/findings/{id}/remediation/plan — BUG-007, BUG-008 fixed
    @PostMapping("/findings/{id}/remediation/plan")
    @Operation(summary = "Create a remediation plan for a finding")
    public ResponseEntity<ApiResponse<RemediationDTO>> createRemediationPlan(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRemediationPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(remediationService.createRemediationPlan(id, request)));
    }

    // B-068 POST /api/v1/remediation/plans/{id}/validate — BUG-010 fixed
    @PostMapping("/remediation/plans/{id}/validate")
    @Operation(summary = "Validate a remediation plan")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validatePlan(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(remediationService.validatePlan(id)));
    }

    // B-069 GET /api/v1/remediation/plans/{id}
    @GetMapping("/remediation/plans/{id}")
    @Operation(summary = "Get a specific remediation plan")
    public ResponseEntity<ApiResponse<RemediationDTO>> getRemediationPlan(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(remediationService.getRemediationPlan(id)));
    }

    // B-070 POST /api/v1/remediation/plans/{id}/verify — BUG-010 fixed
    @PostMapping("/remediation/plans/{id}/verify")
    @Operation(summary = "Verify a remediation plan has been applied")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPlan(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(remediationService.verifyPlan(id)));
    }
}
