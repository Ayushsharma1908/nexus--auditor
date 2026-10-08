package com.nexuscomply.drift.dto;

import jakarta.validation.constraints.NotBlank;

/** BUG-009: Request body for POST /api/v1/drift/compare (B-056) */
public class DriftCompareRequest {

    @NotBlank(message = "baseConfigId is required")
    private String baseConfigId;

    @NotBlank(message = "targetConfigId is required")
    private String targetConfigId;

    public DriftCompareRequest() {}

    public String getBaseConfigId()                   { return baseConfigId; }
    public void setBaseConfigId(String baseConfigId)  { this.baseConfigId = baseConfigId; }

    public String getTargetConfigId()                     { return targetConfigId; }
    public void setTargetConfigId(String targetConfigId)  { this.targetConfigId = targetConfigId; }
}
