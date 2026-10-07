package com.nexuscomply.cyber.report;

import com.nexuscomply.cyber.ai.persistence.AiMappingDocument;
import com.nexuscomply.cyber.audit.AuditSummary;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanDocument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured audit report containing findings with evidence, remediation plans,
 * pending unknowns, coverage metrics, and a formatted Markdown rendering.
 */
public class AuditReportResponse {

    private String auditId;
    private String deviceId;
    private String configurationId;
    private String versionId;
    private String vendor;
    private String platform;
    private String status;
    private Instant startedAt;
    private Instant completedAt;

    private AuditSummary summary;
    private Double coverage;
    private int unknownCount;
    private int notApplicableCount;
    private Double complianceScore;

    private List<FindingReportEntry> findings = new ArrayList<>();
    private List<RemediationPlanDocument> remediationPlans = new ArrayList<>();
    private List<AiMappingDocument> pendingUnknownSyntax = new ArrayList<>();

    private String markdownReport;

    public AuditReportResponse() {}

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public AuditSummary getSummary() {
        return summary;
    }

    public void setSummary(AuditSummary summary) {
        this.summary = summary;
    }

    public Double getCoverage() {
        return coverage;
    }

    public void setCoverage(Double coverage) {
        this.coverage = coverage;
    }

    public int getUnknownCount() {
        return unknownCount;
    }

    public void setUnknownCount(int unknownCount) {
        this.unknownCount = unknownCount;
    }

    public int getNotApplicableCount() {
        return notApplicableCount;
    }

    public void setNotApplicableCount(int notApplicableCount) {
        this.notApplicableCount = notApplicableCount;
    }

    public Double getComplianceScore() {
        return complianceScore;
    }

    public void setComplianceScore(Double complianceScore) {
        this.complianceScore = complianceScore;
    }

    public List<FindingReportEntry> getFindings() {
        return findings;
    }

    public void setFindings(List<FindingReportEntry> findings) {
        this.findings = findings != null ? findings : new ArrayList<>();
    }

    public List<RemediationPlanDocument> getRemediationPlans() {
        return remediationPlans;
    }

    public void setRemediationPlans(List<RemediationPlanDocument> remediationPlans) {
        this.remediationPlans = remediationPlans != null ? remediationPlans : new ArrayList<>();
    }

    public List<AiMappingDocument> getPendingUnknownSyntax() {
        return pendingUnknownSyntax;
    }

    public void setPendingUnknownSyntax(List<AiMappingDocument> pendingUnknownSyntax) {
        this.pendingUnknownSyntax = pendingUnknownSyntax != null ? pendingUnknownSyntax : new ArrayList<>();
    }

    public String getMarkdownReport() {
        return markdownReport;
    }

    public void setMarkdownReport(String markdownReport) {
        this.markdownReport = markdownReport;
    }
}
