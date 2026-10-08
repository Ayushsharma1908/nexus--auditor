package com.nexuscomply.audit.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditScope {
    private List<String> deviceIds = new ArrayList<>();
    private List<String> configurationIds = new ArrayList<>();
    private List<String> frameworkIds = new ArrayList<>();

    public AuditScope() {}

    public AuditScope(List<String> deviceIds, List<String> configurationIds, List<String> frameworkIds) {
        this.deviceIds = deviceIds != null ? deviceIds : new ArrayList<>();
        this.configurationIds = configurationIds != null ? configurationIds : new ArrayList<>();
        this.frameworkIds = frameworkIds != null ? frameworkIds : new ArrayList<>();
    }

    public List<String> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(List<String> deviceIds) {
        this.deviceIds = deviceIds != null ? deviceIds : new ArrayList<>();
    }

    public List<String> getConfigurationIds() {
        return configurationIds;
    }

    public void setConfigurationIds(List<String> configurationIds) {
        this.configurationIds = configurationIds != null ? configurationIds : new ArrayList<>();
    }

    public List<String> getFrameworkIds() {
        return frameworkIds;
    }

    public void setFrameworkIds(List<String> frameworkIds) {
        this.frameworkIds = frameworkIds != null ? frameworkIds : new ArrayList<>();
    }
}
