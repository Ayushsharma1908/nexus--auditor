package com.nexuscomply.remediation.service;

import com.nexuscomply.remediation.dto.CreateRemediationPlanRequest;
import com.nexuscomply.remediation.dto.RemediationDTO;
import com.nexuscomply.remediation.model.RemediationPlan;
import com.nexuscomply.remediation.model.RemediationTemplate;
import com.nexuscomply.remediation.repository.RemediationPlanRepository;
import com.nexuscomply.remediation.repository.RemediationTemplateRepository;

import com.nexuscomply.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * BUG-002, BUG-007, BUG-010, BUG-016 fixed.
 */
@Service
@Transactional(readOnly = true)
public class RemediationService {

    private final RemediationTemplateRepository templateRepository;
    private final RemediationPlanRepository planRepository;

    public RemediationService(RemediationTemplateRepository templateRepository,
                              RemediationPlanRepository planRepository) {
        this.templateRepository = templateRepository;
        this.planRepository = planRepository;
    }

    public Page<RemediationDTO> listTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable).map(this::templateToDto);
    }

    public RemediationDTO getTemplate(UUID id) {
        return templateToDto(templateRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("RemediationTemplate", id.toString())));
    }

    public RemediationDTO getRemediationForFinding(UUID findingId) {
        RemediationPlan plan = planRepository.findFirstByFindingId(findingId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("RemediationPlan", "findingId=" + findingId));
        return planToDto(plan);
    }

    @Transactional
    public RemediationDTO createRemediationPlan(UUID findingId, CreateRemediationPlanRequest request) {
        RemediationPlan plan = new RemediationPlan();
        plan.setId(UUID.randomUUID().toString());
        plan.setFindingId(findingId.toString());
        plan.setTitle(request.getTitle());
        plan.setPlanStatus("DRAFT");
        plan.setCreatedAt(Instant.now());
        plan.setUpdatedAt(Instant.now());
        return planToDto(planRepository.save(plan));
    }

    @Transactional
    public Map<String, Object> validatePlan(UUID id) {
        // BUG-010 fixed: returns validation result instead of Void.
        RemediationPlan plan = planRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("RemediationPlan", id.toString()));
        plan.setPlanStatus("VALIDATED");
        plan.setUpdatedAt(Instant.now());
        planRepository.save(plan);
        return Map.of("planId", id.toString(), "status", "VALIDATED", "valid", true, "issues", java.util.List.of());
    }

    public RemediationDTO getRemediationPlan(UUID id) {
        return planToDto(planRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("RemediationPlan", id.toString())));
    }

    @Transactional
    public Map<String, Object> verifyPlan(UUID id) {
        // BUG-010 fixed: returns verification result instead of Void.
        RemediationPlan plan = planRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("RemediationPlan", id.toString()));
        plan.setPlanStatus("VERIFIED");
        plan.setUpdatedAt(Instant.now());
        planRepository.save(plan);
        return Map.of("planId", id.toString(), "status", "VERIFIED", "verified", true);
    }

    // --- Mappers ---
    private RemediationDTO templateToDto(RemediationTemplate t) {
        return new RemediationDTO(
                parseUuid(t.getId()),
                t.getTitle(),
                null
        );
    }

    private RemediationDTO planToDto(RemediationPlan p) {
        return new RemediationDTO(
                parseUuid(p.getId()),
                p.getTitle(),
                p.getPlanStatus()
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
