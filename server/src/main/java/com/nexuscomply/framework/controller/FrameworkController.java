package com.nexuscomply.framework.controller;

import com.nexuscomply.common.api.ApiResponse;
import com.nexuscomply.common.security.RequestIdFilter;
import com.nexuscomply.framework.model.ComplianceRule;
import com.nexuscomply.framework.model.Control;
import com.nexuscomply.framework.model.Framework;
import com.nexuscomply.framework.model.FrameworkMapping;
import com.nexuscomply.framework.service.FrameworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Frameworks & Controls", description = "Framework, control, rule and cross-mapping queries")
public class FrameworkController {

    private final FrameworkService frameworkService;

    public FrameworkController(FrameworkService frameworkService) {
        this.frameworkService = frameworkService;
    }

    /**
     * B-009: GET /api/v1/frameworks
     * List frameworks.
     */
    @GetMapping("/frameworks")
    @Operation(summary = "List frameworks", description = "Retrieves all security and compliance frameworks.")
    public ResponseEntity<ApiResponse<List<Framework>>> listFrameworks() {
        List<Framework> frameworks = frameworkService.getAllFrameworks();
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(frameworks, requestId));
    }

    /**
     * B-010: GET /api/v1/frameworks/{id}
     * Get framework.
     */
    @GetMapping("/frameworks/{id}")
    @Operation(summary = "Get framework", description = "Retrieves framework details by ID.")
    public ResponseEntity<ApiResponse<Framework>> getFramework(@PathVariable("id") String id) {
        Framework framework = frameworkService.getFrameworkById(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(framework, requestId));
    }

    /**
     * B-011: GET /api/v1/frameworks/{id}/controls
     * List framework controls.
     */
    @GetMapping("/frameworks/{id}/controls")
    @Operation(summary = "List framework controls", description = "Retrieves all controls belonging to a framework.")
    public ResponseEntity<ApiResponse<List<Control>>> listFrameworkControls(@PathVariable("id") String id) {
        List<Control> controls = frameworkService.getControlsByFramework(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(controls, requestId));
    }

    /**
     * B-012: GET /api/v1/controls/{id}
     * Get control.
     */
    @GetMapping("/controls/{id}")
    @Operation(summary = "Get control", description = "Retrieves control details by ID.")
    public ResponseEntity<ApiResponse<Control>> getControl(@PathVariable("id") String id) {
        Control control = frameworkService.getControlById(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(control, requestId));
    }

    /**
     * B-013: GET /api/v1/controls/{id}/rules
     * List rules for control.
     */
    @GetMapping("/controls/{id}/rules")
    @Operation(summary = "List rules for control", description = "Retrieves compliance rules executing this control.")
    public ResponseEntity<ApiResponse<List<ComplianceRule>>> listRulesForControl(@PathVariable("id") String id) {
        List<ComplianceRule> rules = frameworkService.getRulesByControl(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(rules, requestId));
    }

    /**
     * B-014: GET /api/v1/rules/{id}
     * Get rule.
     */
    @GetMapping("/rules/{id}")
    @Operation(summary = "Get rule", description = "Retrieves compliance rule details by ID.")
    public ResponseEntity<ApiResponse<ComplianceRule>> getRule(@PathVariable("id") String id) {
        ComplianceRule rule = frameworkService.getRuleById(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(rule, requestId));
    }

    /**
     * B-015: GET /api/v1/framework-mappings
     * List framework mappings.
     */
    @GetMapping("/framework-mappings")
    @Operation(summary = "List framework mappings", description = "Retrieves all cross-framework mappings.")
    public ResponseEntity<ApiResponse<List<FrameworkMapping>>> listFrameworkMappings() {
        List<FrameworkMapping> mappings = frameworkService.getAllMappings();
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(mappings, requestId));
    }

    /**
     * B-016: GET /api/v1/framework-mappings/{id}
     * Get framework mapping.
     */
    @GetMapping("/framework-mappings/{id}")
    @Operation(summary = "Get framework mapping", description = "Retrieves cross-framework mapping details by ID.")
    public ResponseEntity<ApiResponse<FrameworkMapping>> getFrameworkMapping(@PathVariable("id") String id) {
        FrameworkMapping mapping = frameworkService.getMappingById(id);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(mapping, requestId));
    }
}
