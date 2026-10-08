package com.nexuscomply.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportDTO {
    private UUID id;
    private String rawId;
    private String name;
    private String title;
    private String type = "Executive Summary";
    private String deviceId;
    private String auditId;
    private String date;
    private String status;
    private Integer compliance = 100;
    private String downloadUrl;
    private Instant createdAt;

    public ReportDTO() {}

    public ReportDTO(UUID id, String name, String status, String downloadUrl, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.title = name;
        this.status = status;
        this.downloadUrl = downloadUrl;
        this.createdAt = createdAt;
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

    public String getName() {
        return name != null ? name : title;
    }

    public void setName(String name) {
        this.name = name;
        if (this.title == null) this.title = name;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title != null ? title : name;
    }

    public void setTitle(String title) {
        this.title = title;
        if (this.name == null) this.name = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    @JsonProperty("date")
    public String getDate() {
        if (date != null && !date.isEmpty()) return date;
        if (createdAt != null) return createdAt.toString().substring(0, 10);
        return null;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCompliance() {
        return compliance != null ? compliance : 100;
    }

    public void setCompliance(Integer compliance) {
        this.compliance = compliance;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
