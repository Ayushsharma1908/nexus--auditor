package com.nexuscomply.compliance.service;

import com.nexuscomply.compliance.dto.ComplianceSummaryResponse;
import com.nexuscomply.compliance.dto.EvaluateComplianceRequest;
import com.nexuscomply.compliance.model.ComplianceResult;
import com.nexuscomply.compliance.model.UnknownComplianceItem;

import java.util.List;

public interface ComplianceService {
    List<ComplianceResult> evaluateCompliance(EvaluateComplianceRequest request);
    ComplianceResult evaluateControl(String controlId, EvaluateComplianceRequest request);
    List<ComplianceResult> evaluateFramework(String frameworkId, EvaluateComplianceRequest request);
    List<ComplianceResult> getResultsByAudit(String auditId);
    ComplianceSummaryResponse getSummaryByAudit(String auditId);
    List<UnknownComplianceItem> getUnknownsByAudit(String auditId);
}
