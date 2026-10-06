package com.nexuscomply.cyber.report;

import com.nexuscomply.cyber.audit.AuditSummary;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated compliance and security posture for a single device.
 * Computed on read from operational collections; persists nothing.
 */
public class DevicePostureResponse {

    private String deviceId;
    private String vendor;
    private String platform;
    private AuditSummary latestAuditSummary;
    private Double complianceScore;
    private Double coverage;
    private int openFindingsCount;
    private Map<String, Long> openFindingsBySeverity = new LinkedHashMap<>();
    private Map<String, Long> openFindingsByFramework = new LinkedHashMap<>();
    private List<RiskAssessmentDocument> topRiskAssessments = new ArrayList<>();

    public DevicePostureResponse() {}

    public DevicePostureResponse(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    public AuditSummary getLatestAuditSummary() {
        return latestAuditSummary;
    }

    public void setLatestAuditSummary(AuditSummary latestAuditSummary) {
        this.latestAuditSummary = latestAuditSummary;
    }

    public Double getComplianceScore() {
        return complianceScore;
    }

    public void setComplianceScore(Double complianceScore) {
        this.complianceScore = complianceScore;
    }

    public Double getCoverage() {
        return coverage;
    }

    public void setCoverage(Double coverage) {
        this.coverage = coverage;
    }

    public int getOpenFindingsCount() {
        return openFindingsCount;
    }

    public void setOpenFindingsCount(int openFindingsCount) {
        this.openFindingsCount = openFindingsCount;
    }

    public Map<String, Long> getOpenFindingsBySeverity() {
        return openFindingsBySeverity;
    }

    public void setOpenFindingsBySeverity(Map<String, Long> openFindingsBySeverity) {
        this.openFindingsBySeverity = openFindingsBySeverity != null ? openFindingsBySeverity : new LinkedHashMap<>();
    }

    public Map<String, Long> getOpenFindingsByFramework() {
        return openFindingsByFramework;
    }

    public void setOpenFindingsByFramework(Map<String, Long> openFindingsByFramework) {
        this.openFindingsByFramework = openFindingsByFramework != null ? openFindingsByFramework : new LinkedHashMap<>();
    }

    public List<RiskAssessmentDocument> getTopRiskAssessments() {
        return topRiskAssessments;
    }

    public void setTopRiskAssessments(List<RiskAssessmentDocument> topRiskAssessments) {
        this.topRiskAssessments = topRiskAssessments != null ? topRiskAssessments : new ArrayList<>();
    }
}
