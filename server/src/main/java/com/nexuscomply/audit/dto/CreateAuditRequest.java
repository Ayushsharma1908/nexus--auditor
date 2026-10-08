package com.nexuscomply.audit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateAuditRequest {
    private String name;
    private String type;
    private List<String> deviceIds;
    private List<String> configurationIds;
    private List<String> frameworkIds;

    public CreateAuditRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(List<String> deviceIds) {
        this.deviceIds = deviceIds;
    }

    public List<String> getConfigurationIds() {
        return configurationIds;
    }

    public void setConfigurationIds(List<String> configurationIds) {
        this.configurationIds = configurationIds;
    }

    public List<String> getFrameworkIds() {
        return frameworkIds;
    }

    public void setFrameworkIds(List<String> frameworkIds) {
        this.frameworkIds = frameworkIds;
    }
}
