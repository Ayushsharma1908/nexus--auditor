package com.nexuscomply.cyber.simulation;

/**
 * Sub-document representing compliance/risk posture before and after simulation (schema1.md section 14).
 */
public class SimulationPosture {

    private Double complianceScore;
    private Integer riskScore;
    private Integer failedControls;
    private Integer passedControls;

    public SimulationPosture() {}

    public SimulationPosture(Double complianceScore, Integer riskScore, Integer failedControls) {
        this(complianceScore, riskScore, failedControls, null);
    }

    public SimulationPosture(Double complianceScore, Integer riskScore, Integer failedControls, Integer passedControls) {
        this.complianceScore = complianceScore;
        this.riskScore = riskScore;
        this.failedControls = failedControls;
        this.passedControls = passedControls;
    }

    public Integer getPassedControls() {
        return passedControls;
    }

    public void setPassedControls(Integer passedControls) {
        this.passedControls = passedControls;
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
