package com.nexuscomply.report.service;

import com.nexuscomply.report.dto.CreateReportRequest;
import com.nexuscomply.report.dto.ReportDTO;
import com.nexuscomply.report.model.Report;
import com.nexuscomply.report.repository.ReportRepository;

import com.nexuscomply.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * BUG-002, BUG-007, BUG-016 fixed.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional
    public ReportDTO createReport(CreateReportRequest request) {
        Report report = new Report();
        report.setId(UUID.randomUUID().toString());
        report.setName(request.getName());
        report.setStatus("QUEUED");
        report.setCreatedAt(Instant.now());
        report.setUpdatedAt(Instant.now());
        return toDto(reportRepository.save(report));
    }

    public Page<ReportDTO> listReports(Pageable pageable) {
        return reportRepository.findAll(pageable).map(this::toDto);
    }

    public ReportDTO getReport(UUID id) {
        return toDto(reportRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Report", id.toString())));
    }

    public Map<String, Object> previewReport(UUID id) {
        Report report = reportRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Report", id.toString()));
        return Map.of("reportId", id.toString(), "name", report.getName(), "status", report.getStatus(), "preview", "");
    }

    public Map<String, Object> downloadReport(UUID id) {
        Report report = reportRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Report", id.toString()));
        if (!"READY".equals(report.getStatus())) {
            throw new IllegalStateException("Report is not ready for download. Current status: " + report.getStatus());
        }
        return Map.of("reportId", id.toString(), "downloadUrl", report.getDownloadUrl());
    }

    @Transactional
    public ReportDTO regenerateReport(UUID id) {
        Report report = reportRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Report", id.toString()));
        report.setStatus("QUEUED");
        report.setUpdatedAt(Instant.now());
        return toDto(reportRepository.save(report));
    }

    // --- Mapper ---
    private ReportDTO toDto(Report r) {
        ReportDTO dto = new ReportDTO(
                parseUuid(r.getId()),
                r.getName(),
                r.getStatus(),
                r.getDownloadUrl(),
                r.getCreatedAt()
        );
        dto.setRawId(r.getId());
        dto.setTitle(r.getTitle());
        dto.setType(r.getType());
        dto.setDeviceId(r.getDeviceId());
        dto.setAuditId(r.getAuditId());
        dto.setDate(r.getDate());
        dto.setCompliance(r.getCompliance());
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
