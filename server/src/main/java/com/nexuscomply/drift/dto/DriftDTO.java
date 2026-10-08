package com.nexuscomply.drift.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DriftDTO {
    private UUID id;
    private UUID deviceId;
    private String rawId;
    private String rawDeviceId;
    private String description;
    private String change;
    private String driftStatus;
    private Instant detectedAt;
    private Integer version = 1;
    private String date;
    private String impact = "Increased";
    private List<String> controls = new ArrayList<>();
    private Integer riskBefore = 35;
    private Integer riskAfter = 48;
    private String finding;

    public DriftDTO() {}

    public DriftDTO(UUID id, UUID deviceId, String description, String driftStatus, Instant detectedAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.description = description;
        this.change = description;
        this.driftStatus = driftStatus;
        this.detectedAt = detectedAt;
    }

    public DriftDTO(String rawId, String rawDeviceId, Integer version, String date, String change,
                    String impact, List<String> controls, Integer riskBefore, Integer riskAfter,
                    String finding, String driftStatus, Instant detectedAt) {
        this.rawId = rawId;
        this.rawDeviceId = rawDeviceId;
        this.version = version;
        this.date = date;
        this.change = change;
        this.description = change;
        this.impact = impact;
        this.controls = controls != null ? controls : new ArrayList<>();
        this.riskBefore = riskBefore;
        this.riskAfter = riskAfter;
        this.finding = finding;
        this.driftStatus = driftStatus;
        this.detectedAt = detectedAt;
    }

    @JsonProperty("id")
    public String getIdString() {
        return rawId != null ? rawId : (id != null ? id.toString() : null);
    }

    @JsonIgnore
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRawId() {
        return rawId;
    }

    public void setRawId(String rawId) {
        this.rawId = rawId;
    }

    @JsonProperty("deviceId")
    public String getDeviceIdString() {
        return rawDeviceId != null ? rawDeviceId : (deviceId != null ? deviceId.toString() : null);
    }

    @JsonIgnore
    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public String getRawDeviceId() {
        return rawDeviceId;
    }

    public void setRawDeviceId(String rawDeviceId) {
        this.rawDeviceId = rawDeviceId;
    }

    public String getDescription() {
        return description != null ? description : change;
    }

    public void setDescription(String description) {
        this.description = description;
        if (this.change == null) this.change = description;
    }

    @JsonProperty("change")
    public String getChange() {
        return change != null ? change : description;
    }

    public void setChange(String change) {
        this.change = change;
        if (this.description == null) this.description = change;
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

    public Integer getVersion() {
        return version != null ? version : 1;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @JsonProperty("date")
    public String getDate() {
        if (date != null && !date.isEmpty()) return date;
        if (detectedAt != null) return detectedAt.toString().substring(0, 10);
        return null;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getImpact() {
        return impact != null ? impact : "Increased";
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public List<String> getControls() {
        return controls != null ? controls : new ArrayList<>();
    }

    public void setControls(List<String> controls) {
        this.controls = controls != null ? controls : new ArrayList<>();
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
