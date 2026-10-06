package com.nexuscomply.cyber.simulation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of a What-If simulation execution.
 */
public class WhatIfSimulationResult {

    private String deviceId;
    private String baseConfigurationVersionId;
    private String status = "COMPLETED";

    private SimulationPosture before = new SimulationPosture();
    private SimulationPosture after = new SimulationPosture();

    private int findingDelta; // afterFailed - beforeFailed
    private int riskDelta;    // afterRisk - beforeRisk
    private String impact;    // INCREASED, DECREASED, MIXED, NO_CHANGE, UNKNOWN

    private List<SimulationChange> changes = new ArrayList<>();
    private List<String> affectedControlIds = new ArrayList<>();
    private List<String> affectedFindingIds = new ArrayList<>();

    private Map<String, String> ruleResultsBefore = new LinkedHashMap<>();
    private Map<String, String> ruleResultsAfter = new LinkedHashMap<>();

    private String persistedSimulationId;

    public WhatIfSimulationResult() {}

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public SimulationPosture getBefore() {
        return before;
    }

    public void setBefore(SimulationPosture before) {
        this.before = before != null ? before : new SimulationPosture();
    }

    public SimulationPosture getAfter() {
        return after;
    }

    public void setAfter(SimulationPosture after) {
        this.after = after != null ? after : new SimulationPosture();
    }

    public int getFindingDelta() {
        return findingDelta;
    }

    public void setFindingDelta(int findingDelta) {
        this.findingDelta = findingDelta;
    }

    public int getRiskDelta() {
        return riskDelta;
    }

    public void setRiskDelta(int riskDelta) {
        this.riskDelta = riskDelta;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public List<SimulationChange> getChanges() {
        return changes;
    }

    public void setChanges(List<SimulationChange> changes) {
        this.changes = changes != null ? changes : new ArrayList<>();
    }

    public List<String> getAffectedControlIds() {
        return affectedControlIds;
    }

    public void setAffectedControlIds(List<String> affectedControlIds) {
        this.affectedControlIds = affectedControlIds != null ? affectedControlIds : new ArrayList<>();
    }

    public List<String> getAffectedFindingIds() {
        return affectedFindingIds;
    }

    public void setAffectedFindingIds(List<String> affectedFindingIds) {
        this.affectedFindingIds = affectedFindingIds != null ? affectedFindingIds : new ArrayList<>();
    }

    public Map<String, String> getRuleResultsBefore() {
        return ruleResultsBefore;
    }

    public void setRuleResultsBefore(Map<String, String> ruleResultsBefore) {
        this.ruleResultsBefore = ruleResultsBefore != null ? ruleResultsBefore : new LinkedHashMap<>();
    }

    public Map<String, String> getRuleResultsAfter() {
        return ruleResultsAfter;
    }

    public void setRuleResultsAfter(Map<String, String> ruleResultsAfter) {
        this.ruleResultsAfter = ruleResultsAfter != null ? ruleResultsAfter : new LinkedHashMap<>();
    }

    public String getPersistedSimulationId() {
        return persistedSimulationId;
    }

    public void setPersistedSimulationId(String persistedSimulationId) {
        this.persistedSimulationId = persistedSimulationId;
    }
}
