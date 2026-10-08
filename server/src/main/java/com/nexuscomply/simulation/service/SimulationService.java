package com.nexuscomply.simulation.service;

import com.nexuscomply.simulation.dto.CreateSimulationRequest;
import com.nexuscomply.simulation.dto.SimulationDTO;
import com.nexuscomply.simulation.model.Simulation;
import com.nexuscomply.simulation.repository.SimulationRepository;

import com.nexuscomply.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * BUG-002, BUG-007, BUG-008, BUG-016 fixed.
 */
@Service
@Transactional(readOnly = true)
public class SimulationService {

    private final SimulationRepository simulationRepository;

    public SimulationService(SimulationRepository simulationRepository) {
        this.simulationRepository = simulationRepository;
    }

    @Transactional
    public SimulationDTO createSimulation(CreateSimulationRequest request) {
        Simulation sim = new Simulation();
        sim.setId(UUID.randomUUID().toString());
        sim.setName(request.getName());
        sim.setStatus("QUEUED");
        sim.setCreatedAt(Instant.now());
        sim.setUpdatedAt(Instant.now());
        return toDto(simulationRepository.save(sim));
    }

    public Page<SimulationDTO> listSimulations(Pageable pageable) {
        return simulationRepository.findAll(pageable).map(this::toDto);
    }

    public SimulationDTO getSimulation(UUID id) {
        return toDto(simulationRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Simulation", id.toString())));
    }

    @Transactional
    public SimulationDTO rerunSimulation(UUID id) {
        Simulation sim = simulationRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Simulation", id.toString()));
        sim.setStatus("QUEUED");
        sim.setUpdatedAt(Instant.now());
        return toDto(simulationRepository.save(sim));
    }

    @Transactional
    public void deleteSimulation(UUID id) {
        if (!simulationRepository.existsById(id.toString())) {
            throw new ResourceNotFoundException("Simulation", id.toString());
        }
        simulationRepository.deleteById(id.toString());
    }

    // --- Mapper ---
    private SimulationDTO toDto(Simulation s) {
        return new SimulationDTO(
                parseUuid(s.getId()),
                s.getName(),
                s.getStatus(),
                s.getCreatedAt()
        );
    }

    private UUID parseUuid(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException ex) {
            return UUID.nameUUIDFromBytes(str.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
