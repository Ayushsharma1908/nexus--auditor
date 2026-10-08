package com.nexuscomply.audit.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditSummary {
    private Double complianceScore = 100.0;
    private Integer totalControls = 0;
    private Integer passedControls = 0;
    private Integer failedControls = 0;
    private Integer unknownControls = 0;
    private Integer totalFindings = 0;
    private Integer criticalFindings = 0;
    private Integer highFindings = 0;
    private Integer mediumFindings = 0;
    private Integer lowFindings = 0;

    public AuditSummary() {}

    public Double getComplianceScore() {
        return complianceScore;
    }

    public void setComplianceScore(Double complianceScore) {
        this.complianceScore = complianceScore;
    }

    public Integer getTotalControls() {
        return totalControls;
    }

    public void setTotalControls(Integer totalControls) {
        this.totalControls = totalControls;
    }

    public Integer getPassedControls() {
        return passedControls;
    }

    public void setPassedControls(Integer passedControls) {
        this.passedControls = passedControls;
    }

    public Integer getFailedControls() {
        return failedControls;
    }

    public void setFailedControls(Integer failedControls) {
        this.failedControls = failedControls;
    }

    public Integer getUnknownControls() {
        return unknownControls;
    }

    public void setUnknownControls(Integer unknownControls) {
        this.unknownControls = unknownControls;
    }

    public Integer getTotalFindings() {
        return totalFindings;
    }

    public void setTotalFindings(Integer totalFindings) {
        this.totalFindings = totalFindings;
    }

    public Integer getCriticalFindings() {
        return criticalFindings;
    }

    public void setCriticalFindings(Integer criticalFindings) {
        this.criticalFindings = criticalFindings;
    }

    public Integer getHighFindings() {
        return highFindings;
    }

    public void setHighFindings(Integer highFindings) {
        this.highFindings = highFindings;
    }

    public Integer getMediumFindings() {
        return mediumFindings;
    }

    public void setMediumFindings(Integer mediumFindings) {
        this.mediumFindings = mediumFindings;
    }

    public Integer getLowFindings() {
        return lowFindings;
    }

    public void setLowFindings(Integer lowFindings) {
        this.lowFindings = lowFindings;
    }
}
