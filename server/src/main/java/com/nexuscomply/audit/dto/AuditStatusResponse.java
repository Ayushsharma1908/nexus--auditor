package com.nexuscomply.audit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditStatusResponse {
    private String auditId;
    private String status;
    private Integer progressPercent;
    private String message;

    public AuditStatusResponse() {}

    public AuditStatusResponse(String auditId, String status, Integer progressPercent, String message) {
        this.auditId = auditId;
        this.status = status;
        this.progressPercent = progressPercent;
        this.message = message;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
