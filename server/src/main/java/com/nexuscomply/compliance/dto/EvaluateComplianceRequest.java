package com.nexuscomply.compliance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvaluateComplianceRequest {
    private String auditId;
    private String configurationId;
    private String deviceId;
    private List<String> frameworkIds;
    private List<String> controlIds;

    public EvaluateComplianceRequest() {}

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public List<String> getFrameworkIds() {
        return frameworkIds;
    }

    public void setFrameworkIds(List<String> frameworkIds) {
        this.frameworkIds = frameworkIds;
    }

    public List<String> getControlIds() {
        return controlIds;
    }

    public void setControlIds(List<String> controlIds) {
        this.controlIds = controlIds;
    }
}
