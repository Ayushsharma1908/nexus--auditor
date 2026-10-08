package com.nexuscomply.dashboard.dto;

import java.util.Map;

/**
 * BUG-012 / BUG-019 fixed: fields are now strongly typed as Map<String, Object>
 * instead of raw Object. DashboardController endpoints each return one section.
 * This class is kept for potential future single-payload aggregation use.
 */
public class DashboardDTO {

    private Map<String, Object> summary;
    private Map<String, Object> complianceMetrics;
    private Map<String, Object> findingStats;
    private Map<String, Object> riskTrend;
    private Map<String, Object> driftStats;
    private Map<String, Object> recentActivity;
    private Map<String, Object> frameworkProgress;

    public DashboardDTO() {}

    public Map<String, Object> getSummary()                         { return summary; }
    public void setSummary(Map<String, Object> summary)             { this.summary = summary; }

    public Map<String, Object> getComplianceMetrics()                           { return complianceMetrics; }
    public void setComplianceMetrics(Map<String, Object> complianceMetrics)     { this.complianceMetrics = complianceMetrics; }

    public Map<String, Object> getFindingStats()                        { return findingStats; }
    public void setFindingStats(Map<String, Object> findingStats)       { this.findingStats = findingStats; }

    public Map<String, Object> getRiskTrend()                       { return riskTrend; }
    public void setRiskTrend(Map<String, Object> riskTrend)         { this.riskTrend = riskTrend; }

    public Map<String, Object> getDriftStats()                      { return driftStats; }
    public void setDriftStats(Map<String, Object> driftStats)       { this.driftStats = driftStats; }

    public Map<String, Object> getRecentActivity()                          { return recentActivity; }
    public void setRecentActivity(Map<String, Object> recentActivity)       { this.recentActivity = recentActivity; }

    public Map<String, Object> getFrameworkProgress()                           { return frameworkProgress; }
    public void setFrameworkProgress(Map<String, Object> frameworkProgress)     { this.frameworkProgress = frameworkProgress; }
}
