package com.nexuscomply.compliance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComplianceSummaryResponse {
    private String auditId;
    private Double complianceScore;
    private Integer totalEvaluated;
    private Integer passed;
    private Integer failed;
    private Integer unknowns;

    public ComplianceSummaryResponse() {}

    public ComplianceSummaryResponse(String auditId, Double complianceScore, Integer totalEvaluated, Integer passed, Integer failed, Integer unknowns) {
        this.auditId = auditId;
        this.complianceScore = complianceScore;
        this.totalEvaluated = totalEvaluated;
        this.passed = passed;
        this.failed = failed;
        this.unknowns = unknowns;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public Double getComplianceScore() {
        return complianceScore;
    }

    public void setComplianceScore(Double complianceScore) {
        this.complianceScore = complianceScore;
    }

    public Integer getTotalEvaluated() {
        return totalEvaluated;
    }

    public void setTotalEvaluated(Integer totalEvaluated) {
        this.totalEvaluated = totalEvaluated;
    }

    public Integer getPassed() {
        return passed;
    }

    public void setPassed(Integer passed) {
        this.passed = passed;
    }

    public Integer getFailed() {
        return failed;
    }

    public void setFailed(Integer failed) {
        this.failed = failed;
    }

    public Integer getUnknowns() {
        return unknowns;
    }

    public void setUnknowns(Integer unknowns) {
        this.unknowns = unknowns;
    }
}
