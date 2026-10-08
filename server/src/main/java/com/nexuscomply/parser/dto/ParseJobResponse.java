package com.nexuscomply.parser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexuscomply.parser.model.ParseJob;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParseJobResponse {
    private String jobId;
    private String jobType;
    private String status;
    private String configurationId;
    private String versionId;
    private String deviceId;
    private String vendor;
    private String platform;
    private String message;
    private Integer progressPercent;
    private Instant createdAt;
    private Instant completedAt;

    public ParseJobResponse() {}

    public static ParseJobResponse fromJob(ParseJob job) {
        ParseJobResponse response = new ParseJobResponse();
        response.setJobId(job.getId());
        response.setJobType(job.getJobType());
        response.setStatus(job.getStatus() != null ? job.getStatus().name() : null);
        response.setConfigurationId(job.getConfigurationId());
        response.setVersionId(job.getVersionId());
        response.setDeviceId(job.getDeviceId());
        response.setVendor(job.getVendor());
        response.setPlatform(job.getPlatform());
        response.setMessage(job.getMessage());
        response.setProgressPercent(job.getProgressPercent());
        response.setCreatedAt(job.getCreatedAt());
        response.setCompletedAt(job.getCompletedAt());
        return response;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
