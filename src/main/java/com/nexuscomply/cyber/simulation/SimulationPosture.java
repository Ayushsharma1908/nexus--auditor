package com.nexuscomply.cyber.simulation;

/**
 * Sub-document representing compliance/risk posture before and after simulation (schema1.md section 14).
 */
public class SimulationPosture {

    private Double complianceScore;
    private Integer riskScore;
    private Integer failedControls;

    public SimulationPosture() {}

    public SimulationPosture(Double complianceScore, Integer riskScore, Integer failedControls) {
        this.complianceScore = complianceScore;
        this.riskScore = riskScore;
        this.failedControls = failedControls;
    }

    public Double getComplianceScore() {
        return complianceScore;
    }

    public void setComplianceScore(Double complianceScore) {
        this.complianceScore = complianceScore;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public Integer getFailedControls() {
        return failedControls;
    }

    public void setFailedControls(Integer failedControls) {
        this.failedControls = failedControls;
    }
}
