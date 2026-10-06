package com.nexuscomply.cyber.simulation;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document representation for what_if_simulations collection (schema1.md section 14).
 * Stores proposed changes and their simulated compliance/risk effects.
 *
 * <p>Indexes per schema1.md section 14:
 * deviceId, baseConfigurationVersionId, status, createdBy, createdAt.
 */
@Document(collection = "what_if_simulations")
public class WhatIfSimulationDocument {

    @Id
    private String id;

    @Indexed
    private String deviceId;

    @Indexed
    private String baseConfigurationVersionId;

    private String name;
    private String description;

    private List<SimulationChange> changes = new ArrayList<>();

    @Indexed
    private String status = "COMPLETED";

    private SimulationPosture before = new SimulationPosture();
    private SimulationPosture after = new SimulationPosture();

    private List<String> affectedControlIds = new ArrayList<>();
    private List<String> affectedFindingIds = new ArrayList<>();

    @Indexed
    private String createdBy = "user-uuid";

    @Indexed
    private Instant createdAt;
    private Instant updatedAt;

    public WhatIfSimulationDocument() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<SimulationChange> getChanges() {
        return changes;
    }

    public void setChanges(List<SimulationChange> changes) {
        this.changes = changes != null ? changes : new ArrayList<>();
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
