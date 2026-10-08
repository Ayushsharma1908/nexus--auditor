package com.nexuscomply.ai.dto;

import jakarta.validation.constraints.NotBlank;

/** BUG-007: Request body for POST /api/v1/ai/analyze (B-071) */
public class StartAnalysisRequest {

    @NotBlank(message = "Target resource ID is required")
    private String targetId;

    private String analysisType;  // CONFIGURATION, MAPPING, REMEDIATION

    public StartAnalysisRequest() {}

    public String getTargetId()               { return targetId; }
    public void setTargetId(String targetId)  { this.targetId = targetId; }

    public String getAnalysisType()                   { return analysisType; }
    public void setAnalysisType(String analysisType)  { this.analysisType = analysisType; }
}
