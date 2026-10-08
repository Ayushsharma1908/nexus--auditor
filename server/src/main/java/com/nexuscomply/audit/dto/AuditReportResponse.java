package com.nexuscomply.audit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditReportResponse {
    private String reportId;
    private String auditId;
    private String title;
    private String format;
    private String status;
    private String downloadUrl;
    private Instant generatedAt;

    public AuditReportResponse() {}

    public AuditReportResponse(String reportId, String auditId, String title, String format, String status, String downloadUrl, Instant generatedAt) {
        this.reportId = reportId;
        this.auditId = auditId;
        this.title = title;
        this.format = format;
        this.status = status;
        this.downloadUrl = downloadUrl;
        this.generatedAt = generatedAt;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
