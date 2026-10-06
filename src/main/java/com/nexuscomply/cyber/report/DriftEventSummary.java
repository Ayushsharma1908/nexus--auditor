package com.nexuscomply.cyber.report;

import com.nexuscomply.cyber.drift.model.DriftChange;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary of a single drift event for history reporting.
 */
public class DriftEventSummary {

    private String eventId;
    private Object fromVersion;
    private Object toVersion;
    private String fromVersionId;
    private String toVersionId;
    private Instant detectedAt;
    private String impact;
    private int changesCount;
    private int riskBefore;
    private int riskAfter;
    private int riskDelta;
    private List<DriftChange> changes = new ArrayList<>();

    public DriftEventSummary() {}

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public int getChangesCount() {
        return changesCount;
    }

    public void setChangesCount(int changesCount) {
        this.changesCount = changesCount;
    }

    public int getRiskBefore() {
        return riskBefore;
    }

    public void setRiskBefore(int riskBefore) {
        this.riskBefore = riskBefore;
    }

    public int getRiskAfter() {
        return riskAfter;
    }

    public void setRiskAfter(int riskAfter) {
        this.riskAfter = riskAfter;
    }

    public int getRiskDelta() {
        return riskDelta;
    }

    public void setRiskDelta(int riskDelta) {
        this.riskDelta = riskDelta;
    }

    public List<DriftChange> getChanges() {
        return changes;
    }

    public void setChanges(List<DriftChange> changes) {
        this.changes = changes != null ? changes : new ArrayList<>();
    }
}
