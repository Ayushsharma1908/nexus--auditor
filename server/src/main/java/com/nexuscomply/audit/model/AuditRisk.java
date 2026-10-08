package com.nexuscomply.audit.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditRisk {
    private Double overallRiskScore = 0.0;
    private String riskLevel = "LOW";
    private Integer criticalCount = 0;
    private Integer highCount = 0;
    private Integer mediumCount = 0;
    private Integer lowCount = 0;

    public AuditRisk() {}

    public Double getOverallRiskScore() {
        return overallRiskScore;
    }

    public void setOverallRiskScore(Double overallRiskScore) {
        this.overallRiskScore = overallRiskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(Integer criticalCount) {
        this.criticalCount = criticalCount;
    }

    public Integer getHighCount() {
        return highCount;
    }

    public void setHighCount(Integer highCount) {
        this.highCount = highCount;
    }

    public Integer getMediumCount() {
        return mediumCount;
    }

    public void setMediumCount(Integer mediumCount) {
        this.mediumCount = mediumCount;
    }

    public Integer getLowCount() {
        return lowCount;
    }

    public void setLowCount(Integer lowCount) {
        this.lowCount = lowCount;
    }
}
