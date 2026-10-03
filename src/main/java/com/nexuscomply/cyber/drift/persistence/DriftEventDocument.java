package com.nexuscomply.cyber.drift.persistence;

import com.nexuscomply.cyber.drift.model.DriftChange;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "drift_events")
@CompoundIndexes({
        @CompoundIndex(name = "dev_detected_idx", def = "{'deviceId': 1, 'detectedAt': -1}")
})
public class DriftEventDocument {

    @Id
    private String id;

    @Indexed
    private String deviceId;

    @Indexed
    private String fromVersionId;

    @Indexed
    private String toVersionId;

    private Object fromVersion;
    private Object toVersion;

    private List<DriftChange> changes = new ArrayList<>();

    private List<String> affectedControlIds = new ArrayList<>();
    private List<String> affectedFindingIds = new ArrayList<>();

    private Integer riskBefore = 0;
    private Integer riskAfter = 0;

    @Indexed
    private String impact = "NO_CHANGE";

    @Indexed
    private Instant detectedAt;

    private Instant createdAt;
    private Instant updatedAt;

    public DriftEventDocument() {}

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

    public String getFromVersionId() {
        return fromVersionId;
    }

    public void setFromVersionId(String fromVersionId) {
        this.fromVersionId = fromVersionId;
    }

    public String getToVersionId() {
        return toVersionId;
    }

    public void setToVersionId(String toVersionId) {
        this.toVersionId = toVersionId;
    }

    public Object getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(Object fromVersion) {
        this.fromVersion = fromVersion;
    }

    public Object getToVersion() {
        return toVersion;
    }

    public void setToVersion(Object toVersion) {
        this.toVersion = toVersion;
    }

    public List<DriftChange> getChanges() {
        return changes;
    }

    public void setChanges(List<DriftChange> changes) {
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

    public Integer getRiskBefore() {
        return riskBefore;
    }

    public void setRiskBefore(Integer riskBefore) {
        this.riskBefore = riskBefore;
    }

    public Integer getRiskAfter() {
        return riskAfter;
    }

    public void setRiskAfter(Integer riskAfter) {
        this.riskAfter = riskAfter;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
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
