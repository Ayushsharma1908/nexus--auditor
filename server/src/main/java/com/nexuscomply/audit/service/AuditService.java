package com.nexuscomply.audit.service;

import com.nexuscomply.audit.dto.AuditReportResponse;
import com.nexuscomply.audit.dto.AuditStatusResponse;
import com.nexuscomply.audit.dto.CreateAuditRequest;
import com.nexuscomply.audit.model.*;

import java.util.List;

public interface AuditService {
    Audit createAudit(CreateAuditRequest request);
    List<Audit> getAllAudits();
    Audit getAuditById(String id);
    AuditStatusResponse getAuditStatus(String id);
    Audit cancelAudit(String id);
    Audit rerunAudit(String id);
    AuditSummary getAuditSummary(String id);
    List<FrameworkResult> getFrameworkResults(String id);
    List<String> getAuditFindings(String id);
    AuditRisk getAuditRisk(String id);
    AuditReportResponse generateReport(String id);
}
