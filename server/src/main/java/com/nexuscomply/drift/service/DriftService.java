package com.nexuscomply.drift.service;

import com.nexuscomply.drift.dto.DriftCompareRequest;
import com.nexuscomply.drift.dto.DriftDTO;
import com.nexuscomply.drift.model.DriftEvent;
import com.nexuscomply.drift.repository.DriftEventRepository;

import com.nexuscomply.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * BUG-002, BUG-009, BUG-016 fixed: wired to DriftEventRepository.
 */
@Service
@Transactional(readOnly = true)
public class DriftService {

    private final DriftEventRepository driftEventRepository;

    public DriftService(DriftEventRepository driftEventRepository) {
        this.driftEventRepository = driftEventRepository;
    }

    public Page<DriftDTO> listDriftEvents(Pageable pageable) {
        return driftEventRepository.findAll(pageable).map(this::toDto);
    }

    public DriftDTO getDriftEvent(UUID id) {
        DriftEvent event = driftEventRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("DriftEvent", id.toString()));
        return toDto(event);
    }

    public Page<DriftDTO> getDeviceDrift(UUID deviceId, Pageable pageable) {
        return driftEventRepository.findByDeviceId(deviceId.toString(), pageable).map(this::toDto);
    }

    @Transactional
    public DriftDTO compareVersions(DriftCompareRequest request) {
        // BUG-009 fixed: now accepts request body and returns a DriftEvent result.
        // Actual diff computation is Cyber Engine's responsibility; here we record the job.
        DriftEvent event = new DriftEvent();
        event.setId(UUID.randomUUID().toString());
        event.setDescription("Comparing " + request.getBaseConfigId() + " vs " + request.getTargetConfigId());
        event.setDriftStatus("DETECTED");
        event.setDetectedAt(Instant.now());
        return toDto(driftEventRepository.save(event));
    }

    public Map<String, Object> getAffectedControls(UUID driftId) {
        // Validates drift exists, then returns control reference (Cyber Engine provides details).
        driftEventRepository.findById(driftId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("DriftEvent", driftId.toString()));
        return Map.of("driftId", driftId.toString(), "affectedControls", List.of());
    }

    public Map<String, Object> getDriftRiskImpact(UUID driftId) {
        driftEventRepository.findById(driftId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("DriftEvent", driftId.toString()));
        return Map.of("driftId", driftId.toString(), "riskImpact", Map.of());
    }

    // --- Mapper ---
    private DriftDTO toDto(DriftEvent e) {
        DriftDTO dto = new DriftDTO(
                parseUuid(e.getId()),
                parseUuid(e.getDeviceId()),
                e.getDescription(),
                e.getDriftStatus(),
                e.getDetectedAt()
        );
        dto.setRawId(e.getId());
        dto.setRawDeviceId(e.getDeviceId());
        dto.setVersion(e.getVersion());
        dto.setDate(e.getDate());
        dto.setChange(e.getChange());
        dto.setImpact(e.getImpact());
        dto.setControls(e.getControls());
        dto.setRiskBefore(e.getRiskBefore());
        dto.setRiskAfter(e.getRiskAfter());
        dto.setFinding(e.getFinding());
        return dto;
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
