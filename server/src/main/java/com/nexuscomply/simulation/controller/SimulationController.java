package com.nexuscomply.simulation.controller;

import com.nexuscomply.simulation.dto.CreateSimulationRequest;
import com.nexuscomply.simulation.dto.SimulationDTO;
import com.nexuscomply.simulation.service.SimulationService;

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

import java.util.UUID;

/**
 * BUG-006, BUG-007, BUG-008, BUG-015, BUG-018 fixed.
 */
@RestController
@RequestMapping({"/api/v1/simulations", "/api/v1/what-if"})
@Tag(name = "What-If Simulations", description = "What-if simulation endpoints (B-059 → B-063)")
public class SimulationController {

    private final SimulationService simulationService;
    private final com.nexuscomply.cyber.simulation.WhatIfSimulationService cyberWhatIfService;

    public SimulationController(
            SimulationService simulationService,
            @org.springframework.beans.factory.annotation.Qualifier("cyberWhatIfSimulationService")
            com.nexuscomply.cyber.simulation.WhatIfSimulationService cyberWhatIfService) {
        this.simulationService = simulationService;
        this.cyberWhatIfService = cyberWhatIfService;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Execute live cyber engine what-if simulation against dataset")
    public ResponseEntity<ApiResponse<com.nexuscomply.cyber.simulation.WhatIfSimulationResult>> runWhatIf(
            @RequestBody com.nexuscomply.cyber.simulation.WhatIfSimulationRequest request) {
        return ResponseEntity.ok(ApiResponse.of(cyberWhatIfService.simulate(request)));
    }

    // B-059 POST /api/v1/simulations — BUG-007, BUG-008 fixed
    @PostMapping
    @Operation(summary = "Create a new what-if simulation")
    public ResponseEntity<ApiResponse<SimulationDTO>> createSimulation(
            @Valid @RequestBody CreateSimulationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(simulationService.createSimulation(request)));
    }

    // B-060 GET /api/v1/simulations — BUG-006 fixed
    @GetMapping
    @Operation(summary = "List all simulations (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<SimulationDTO>>> listSimulations(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(simulationService.listSimulations(pageable))));
    }

    // B-061 GET /api/v1/simulations/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get a specific simulation")
    public ResponseEntity<ApiResponse<SimulationDTO>> getSimulation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(simulationService.getSimulation(id)));
    }

    // B-062 POST /api/v1/simulations/{id}/rerun
    @PostMapping("/{id}/rerun")
    @Operation(summary = "Re-queue a simulation for re-execution")
    public ResponseEntity<ApiResponse<SimulationDTO>> rerunSimulation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(simulationService.rerunSimulation(id)));
    }

    // B-063 DELETE /api/v1/simulations/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a simulation")
    public ResponseEntity<Void> deleteSimulation(@PathVariable UUID id) {
        simulationService.deleteSimulation(id);
        return ResponseEntity.noContent().build();
    }
}
