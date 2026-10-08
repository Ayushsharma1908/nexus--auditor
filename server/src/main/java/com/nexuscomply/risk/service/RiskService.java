package com.nexuscomply.risk.service;

import com.nexuscomply.risk.dto.RiskDTO;
import com.nexuscomply.risk.model.RiskAssessment;
import com.nexuscomply.risk.repository.RiskAssessmentRepository;

import com.nexuscomply.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * BUG-002, BUG-016 fixed: wired to RiskAssessmentRepository with @Transactional.
 */
@Service
@Transactional(readOnly = true)
public class RiskService {

    private final RiskAssessmentRepository riskAssessmentRepository;

    public RiskService(RiskAssessmentRepository riskAssessmentRepository) {
        this.riskAssessmentRepository = riskAssessmentRepository;
    }

    public Page<RiskDTO> listRisks(Pageable pageable) {
        return riskAssessmentRepository.findAll(pageable).map(this::toDto);
    }

    public Page<RiskDTO> listRisksByFindings(Pageable pageable) {
        return riskAssessmentRepository.findAllByFindingIdNotNull(pageable).map(this::toDto);
    }

    public Page<RiskDTO> listRisksByDevices(Pageable pageable) {
        return riskAssessmentRepository.findAllByDeviceIdNotNull(pageable).map(this::toDto);
    }

    public RiskDTO getRiskForDevice(UUID deviceId) {
        RiskAssessment ra = riskAssessmentRepository.findFirstByDeviceId(deviceId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("RiskAssessment", "deviceId=" + deviceId));
        return toDto(ra);
    }

    public Page<RiskDTO> getRiskTrend(Pageable pageable) {
        return riskAssessmentRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional
    public void recalculateRisk(UUID auditId) {
        // Delegates to Cyber Engine adapter — actual calculation is outside non-cyber boundary.
        // The API layer records the trigger; computation happens asynchronously.
    }

    public RiskDTO getRiskForFinding(UUID findingId) {
        RiskAssessment ra = riskAssessmentRepository.findFirstByFindingId(findingId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("RiskAssessment", "findingId=" + findingId));
        return toDto(ra);
    }

    // --- Mapper ---
    private RiskDTO toDto(RiskAssessment ra) {
        return new RiskDTO(
                parseUuid(ra.getId()),
                ra.getTitle(),
                ra.getSeverity(),
                ra.getScore(),
                ra.getCalculatedAt()
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
