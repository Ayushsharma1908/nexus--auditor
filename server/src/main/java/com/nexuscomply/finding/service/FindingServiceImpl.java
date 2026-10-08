package com.nexuscomply.finding.service;

import com.nexuscomply.common.exception.ResourceNotFoundException;
import com.nexuscomply.finding.dto.UpdateSeverityRequest;
import com.nexuscomply.finding.dto.UpdateStatusRequest;
import com.nexuscomply.finding.model.Evidence;
import com.nexuscomply.finding.model.Finding;
import com.nexuscomply.finding.model.FindingHistoryEntry;
import com.nexuscomply.finding.repository.EvidenceRepository;
import com.nexuscomply.finding.repository.FindingRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class FindingServiceImpl implements FindingService {

    private final FindingRepository findingRepo;
    private final EvidenceRepository evidenceRepo;

    public FindingServiceImpl(FindingRepository findingRepo, EvidenceRepository evidenceRepo) {
        this.findingRepo = findingRepo;
        this.evidenceRepo = evidenceRepo;
    }

    @Override
    public List<Finding> filterFindings(String auditId, String severity, String status, String deviceId) {
        List<Finding> list;
        if (auditId != null && !auditId.isBlank()) {
            list = findingRepo.findByAuditId(auditId);
        } else if (deviceId != null && !deviceId.isBlank()) {
            list = findingRepo.findByDeviceId(deviceId);
        } else if (severity != null && !severity.isBlank()) {
            list = findingRepo.findBySeverity(severity.toUpperCase());
        } else if (status != null && !status.isBlank()) {
            list = findingRepo.findByStatus(status.toUpperCase());
        } else {
            list = findingRepo.findAll();
        }

        return list.stream()
                .filter(f -> auditId == null || auditId.equalsIgnoreCase(f.getAuditId()))
                .filter(f -> severity == null || severity.equalsIgnoreCase(f.getSeverity()))
                .filter(f -> status == null || status.equalsIgnoreCase(f.getStatus()))
                .filter(f -> deviceId == null || deviceId.equalsIgnoreCase(f.getDeviceId()))
                .toList();
    }

    @Override
    public Finding getFindingById(String id) {
        return findingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Finding", id));
    }

    @Override
    public Finding updateFindingStatus(String id, UpdateStatusRequest request) {
        Finding f = getFindingById(id);
        if (request != null && request.getStatus() != null) {
            String oldStatus = f.getStatus();
            f.setStatus(request.getStatus().toUpperCase());
            f.setUpdatedAt(Instant.now());
            String note = request.getNote() != null ? request.getNote() : "Status updated from " + oldStatus + " to " + f.getStatus();
            f.getHistory().add(new FindingHistoryEntry("STATUS_CHANGE", "USER", note));
            return findingRepo.save(f);
        }
        return f;
    }

    @Override
    public Finding updateFindingSeverity(String id, UpdateSeverityRequest request) {
        Finding f = getFindingById(id);
        if (request != null && request.getSeverity() != null) {
            String oldSeverity = f.getSeverity();
            f.setSeverity(request.getSeverity().toUpperCase());
            f.setUpdatedAt(Instant.now());
            String note = request.getNote() != null ? request.getNote() : "Severity updated from " + oldSeverity + " to " + f.getSeverity();
            f.getHistory().add(new FindingHistoryEntry("SEVERITY_OVERRIDE", "USER", note));
            return findingRepo.save(f);
        }
        return f;
    }

    @Override
    public List<FindingHistoryEntry> getFindingHistory(String id) {
        return getFindingById(id).getHistory();
    }

    @Override
    public List<Finding> getRelatedFindings(String id) {
        Finding f = getFindingById(id);
        return findingRepo.findAll().stream()
                .filter(other -> !other.getId().equals(id))
                .filter(other -> (f.getDeviceId() != null && f.getDeviceId().equals(other.getDeviceId()))
                        || (f.getControlId() != null && f.getControlId().equals(other.getControlId())))
                .toList();
    }

    @Override
    public Finding acknowledgeFinding(String id, String note) {
        Finding f = getFindingById(id);
        f.setStatus("ACKNOWLEDGED");
        f.setUpdatedAt(Instant.now());
        f.getHistory().add(new FindingHistoryEntry("ACKNOWLEDGED", "USER", note != null ? note : "Acknowledged by compliance team"));
        return findingRepo.save(f);
    }

    @Override
    public Finding resolveFinding(String id, String note) {
        Finding f = getFindingById(id);
        f.setStatus("RESOLVED");
        f.setUpdatedAt(Instant.now());
        f.getHistory().add(new FindingHistoryEntry("RESOLVED", "USER", note != null ? note : "Marked as resolved with verified configuration change"));
        return findingRepo.save(f);
    }

    @Override
    public List<Evidence> getFindingEvidence(String findingId) {
        getFindingById(findingId); // validate finding exists
        return evidenceRepo.findByFindingId(findingId);
    }

    @Override
    public Evidence getEvidenceById(String id) {
        return evidenceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", id));
    }

    @Override
    public Map<String, Object> getEvidenceSource(String id) {
        Evidence e = getEvidenceById(id);
        Map<String, Object> source = new HashMap<>();
        source.put("evidenceId", e.getId());
        source.put("sourcePath", e.getSourcePath());
        source.put("startLine", e.getStartLine());
        source.put("endLine", e.getEndLine());
        source.put("snippet", e.getSnippet());
        return source;
    }

    @Override
    public Map<String, Object> getEvidenceConfiguration(String id) {
        Evidence e = getEvidenceById(id);
        Map<String, Object> config = new HashMap<>();
        config.put("evidenceId", e.getId());
        config.put("configurationId", e.getConfigurationId());
        config.put("versionId", e.getVersionId());
        config.put("configurationVersionId", e.getConfigurationVersionId());
        config.put("deviceId", e.getDeviceId());
        config.put("snippet", e.getSnippet());
        return config;
    }
}
