package com.nexuscomply.finding.service;

import com.nexuscomply.finding.dto.UpdateSeverityRequest;
import com.nexuscomply.finding.dto.UpdateStatusRequest;
import com.nexuscomply.finding.model.Evidence;
import com.nexuscomply.finding.model.Finding;
import com.nexuscomply.finding.model.FindingHistoryEntry;

import java.util.List;
import java.util.Map;

public interface FindingService {
    List<Finding> filterFindings(String auditId, String severity, String status, String deviceId);
    Finding getFindingById(String id);
    Finding updateFindingStatus(String id, UpdateStatusRequest request);
    Finding updateFindingSeverity(String id, UpdateSeverityRequest request);
    List<FindingHistoryEntry> getFindingHistory(String id);
    List<Finding> getRelatedFindings(String id);
    Finding acknowledgeFinding(String id, String note);
    Finding resolveFinding(String id, String note);
    List<Evidence> getFindingEvidence(String findingId);
    Evidence getEvidenceById(String id);
    Map<String, Object> getEvidenceSource(String id);
    Map<String, Object> getEvidenceConfiguration(String id);
}
