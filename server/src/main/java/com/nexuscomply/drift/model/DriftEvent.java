package com.nexuscomply.drift.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "drift_events")
public class DriftEvent {

    @Id
    private String id;

    @Indexed
    private String deviceId;

    private Integer version = 1;
    private String date;
    private String description;
    private String change;
    private String impact = "Increased"; // Increased | Decreased
    private String driftStatus = "DETECTED";  // DETECTED, ACKNOWLEDGED, RESOLVED
    private Instant detectedAt = Instant.now();

    private List<String> affectedControlIds = new ArrayList<>();
    private List<String> controls = new ArrayList<>();
    private Integer riskBefore = 35;
    private Integer riskAfter = 48;
    private String finding;

    public DriftEvent() {}

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

    public Integer getVersion() {
        return version != null ? version : 1;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDate() {
        if (date != null && !date.isEmpty()) {
            return date;
        }
        if (detectedAt != null) {
            return detectedAt.toString().substring(0, 10);
        }
        return null;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description != null ? description : change;
    }

    public void setDescription(String description) {
        this.description = description;
        if (this.change == null) this.change = description;
    }

    public String getChange() {
        return change != null ? change : description;
    }

    public void setChange(String change) {
        this.change = change;
        if (this.description == null) this.description = change;
    }

    public String getImpact() {
        return impact != null ? impact : "Increased";
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getDriftStatus() {
        return driftStatus;
    }

    public void setDriftStatus(String driftStatus) {
        this.driftStatus = driftStatus;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public List<String> getAffectedControlIds() {
        return affectedControlIds != null ? affectedControlIds : controls;
    }

    public void setAffectedControlIds(List<String> affectedControlIds) {
        this.affectedControlIds = affectedControlIds;
        if (this.controls == null || this.controls.isEmpty()) this.controls = affectedControlIds;
    }

    public List<String> getControls() {
        return controls != null && !controls.isEmpty() ? controls : affectedControlIds;
    }

    public void setControls(List<String> controls) {
        this.controls = controls;
        if (this.affectedControlIds == null || this.affectedControlIds.isEmpty()) this.affectedControlIds = controls;
    }

    public Integer getRiskBefore() {
        return riskBefore != null ? riskBefore : 35;
    }

    public void setRiskBefore(Integer riskBefore) {
        this.riskBefore = riskBefore;
    }

    public Integer getRiskAfter() {
        return riskAfter != null ? riskAfter : 48;
    }

    public void setRiskAfter(Integer riskAfter) {
        this.riskAfter = riskAfter;
    }

    public String getFinding() {
        return finding;
    }

    public void setFinding(String finding) {
        this.finding = finding;
    }
}
