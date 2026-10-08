package com.nexuscomply.report.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "reports")
public class Report {

    @Id
    private String id;

    private String name;
    private String title;
    private String type = "Executive Summary"; // Executive Summary, Findings, Framework Mapping, Risk Assessment
    private String deviceId;
    private String auditId;
    private String date;
    private String status = "Ready"; // Ready, Processing, Archived, READY, GENERATING
    private Integer compliance = 100;
    private String downloadUrl;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Report() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
