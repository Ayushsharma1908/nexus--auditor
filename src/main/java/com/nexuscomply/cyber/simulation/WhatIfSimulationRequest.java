package com.nexuscomply.cyber.simulation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request payload for running a What-If simulation.
 */
public class WhatIfSimulationRequest {

    private String deviceId;
    private String baseConfigurationVersionId;
    private String proposedRawConfig;
    private Map<String, Object> canonicalOverrides = new LinkedHashMap<>();
    private String name = "What-If Simulation";
    private String description;
    private String createdBy = "user-uuid";
    private boolean persist = false;

    public WhatIfSimulationRequest() {}

    public WhatIfSimulationRequest(String deviceId, String baseConfigurationVersionId) {
        this.deviceId = deviceId;
        this.baseConfigurationVersionId = baseConfigurationVersionId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getBaseConfigurationVersionId() {
        return baseConfigurationVersionId;
    }

    public void setBaseConfigurationVersionId(String baseConfigurationVersionId) {
        this.baseConfigurationVersionId = baseConfigurationVersionId;
    }

    public String getProposedRawConfig() {
        return proposedRawConfig;
    }

    public void setProposedRawConfig(String proposedRawConfig) {
        this.proposedRawConfig = proposedRawConfig;
    }

    public Map<String, Object> getCanonicalOverrides() {
        return canonicalOverrides;
    }

    public void setCanonicalOverrides(Map<String, Object> canonicalOverrides) {
        this.canonicalOverrides = canonicalOverrides != null ? canonicalOverrides : new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isPersist() {
        return persist;
    }

    public void setPersist(boolean persist) {
        this.persist = persist;
    }
}
