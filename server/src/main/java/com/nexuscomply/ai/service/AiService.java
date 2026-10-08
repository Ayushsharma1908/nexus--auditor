package com.nexuscomply.ai.service;

import com.nexuscomply.ai.dto.AiDTO;
import com.nexuscomply.ai.dto.StartAnalysisRequest;
import com.nexuscomply.ai.model.AiJob;
import com.nexuscomply.ai.model.AiMapping;
import com.nexuscomply.ai.repository.AiJobRepository;
import com.nexuscomply.ai.repository.AiMappingRepository;

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
 * BUG-002, BUG-007, BUG-008, BUG-011, BUG-016 fixed.
 */
@Service
@Transactional(readOnly = true)
public class AiService {

    private final AiJobRepository aiJobRepository;
    private final AiMappingRepository aiMappingRepository;
    private final com.nexuscomply.cyber.ai.service.GeminiSuggestionProvider geminiSuggestionProvider;

    public AiService(AiJobRepository aiJobRepository, AiMappingRepository aiMappingRepository,
                     @org.springframework.beans.factory.annotation.Autowired(required = false) com.nexuscomply.cyber.ai.service.GeminiSuggestionProvider geminiSuggestionProvider) {
        this.aiJobRepository = aiJobRepository;
        this.aiMappingRepository = aiMappingRepository;
        this.geminiSuggestionProvider = geminiSuggestionProvider;
    }

    /** B-071: Returns a job in QUEUED status — caller should poll B-072. */
    @Transactional
    public AiDTO startAnalysis(StartAnalysisRequest request) {
        AiJob job = new AiJob();
        job.setId(UUID.randomUUID().toString());
        job.setStatus("QUEUED");
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        return toJobDto(aiJobRepository.save(job));
    }

    public AiDTO getJob(UUID id) {
        return toJobDto(aiJobRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("AiJob", id.toString())));
    }

    /** BUG-011: Typed return for suggestions. */
    public Map<String, Object> getSuggestions(UUID id) {
        AiJob job = aiJobRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("AiJob", id.toString()));
        return Map.of("jobId", job.getId(), "status", job.getStatus(), "suggestions", List.of());
    }

    public Page<AiDTO> listMappings(Pageable pageable) {
        return aiMappingRepository.findAll(pageable).map(this::toMappingDto);
    }

    public AiDTO getMapping(String id) {
        AiMapping mapping = aiMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AiMapping", id));
        return toMappingDto(mapping);
    }

    public AiDTO getMapping(UUID id) {
        return getMapping(id.toString());
    }

    @Transactional
    public void approveMapping(String id) {
        AiMapping mapping = aiMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AiMapping", id));
        mapping.setStatus("Approved");
        mapping.setUpdatedAt(Instant.now());
        aiMappingRepository.save(mapping);
    }

    @Transactional
    public void approveMapping(UUID id) {
        approveMapping(id.toString());
    }

    @Transactional
    public void rejectMapping(String id) {
        AiMapping mapping = aiMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AiMapping", id));
        mapping.setStatus("Rejected");
        mapping.setUpdatedAt(Instant.now());
        aiMappingRepository.save(mapping);
    }

    @Transactional
    public void rejectMapping(UUID id) {
        rejectMapping(id.toString());
    }

    /** BUG-011: Typed return for test mapping result. */
    public Map<String, Object> testMapping(String id) {
        AiMapping mapping = aiMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AiMapping", id));
        return Map.of("mappingId", mapping.getId(), "testResult", "PASS", "matchRate", 0.0);
    }

    public Map<String, Object> testMapping(UUID id) {
        return testMapping(id.toString());
    }

    /** BUG-011: Typed return for mapping usage. */
    public Map<String, Object> getMappingUsage(String id) {
        AiMapping mapping = aiMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AiMapping", id));
        return Map.of("mappingId", mapping.getId(), "usageCount", 0, "lastUsed", (Object) null);
    }

    public Map<String, Object> getMappingUsage(UUID id) {
        return getMappingUsage(id.toString());
    }

    public String chat(com.nexuscomply.ai.dto.AiChatRequest request) {
        if (request == null) {
            return "Please provide a valid query.";
        }
        if (geminiSuggestionProvider != null) {
            return geminiSuggestionProvider.chat(request.getUserMessage(), request.getContext(), request.getHistory());
        }
        return "Gemini service is currently unavailable.";
    }

    // --- Mappers ---
    private AiDTO toJobDto(AiJob j) {
        return new AiDTO(
                parseUuid(j.getId()),
                j.getStatus(),
                j.getResult()
        );
    }

    private AiDTO toMappingDto(AiMapping m) {
        AiDTO dto = new AiDTO(
                parseUuid(m.getId()),
                m.getStatus(),
                m.getResult()
        );
        dto.setRawId(m.getId());
        dto.setSyntax(m.getSyntax());
        dto.setVendor(m.getVendor());
        dto.setConfidence(m.getConfidence());
        dto.setCanonicalField(m.getCanonicalField());
        dto.setSuggestedValue(m.getSuggestedValue());
        dto.setReason(m.getReason());
        dto.setReviewer(m.getReviewer());
        dto.setDate(m.getDate());
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
