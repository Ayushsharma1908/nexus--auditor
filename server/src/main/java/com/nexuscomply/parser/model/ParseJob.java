package com.nexuscomply.parser.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "parse_jobs")
public class ParseJob {
    @Id
    private String id;
    private String jobType = "PARSER";
    private ParseJobStatus status = ParseJobStatus.QUEUED;
    private String configurationId;
    private String versionId;
    private String deviceId;
    private String vendor;
    private String platform;
    private String message;
    private int progressPercent = 0;
    private List<ParseError> errors = new ArrayList<>();
    private List<UnknownSyntaxItem> unknowns = new ArrayList<>();
    private Map<String, Object> structuredFacts = new HashMap<>();
    private Instant createdAt = Instant.now();
    private Instant completedAt;

    public ParseJob() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public ParseJobStatus getStatus() {
        return status;
    }

    public void setStatus(ParseJobStatus status) {
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

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public List<ParseError> getErrors() {
        return errors;
    }

    public void setErrors(List<ParseError> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public List<UnknownSyntaxItem> getUnknowns() {
        return unknowns;
    }

    public void setUnknowns(List<UnknownSyntaxItem> unknowns) {
        this.unknowns = unknowns != null ? unknowns : new ArrayList<>();
    }

    public Map<String, Object> getStructuredFacts() {
        return structuredFacts;
    }

    public void setStructuredFacts(Map<String, Object> structuredFacts) {
        this.structuredFacts = structuredFacts != null ? structuredFacts : new HashMap<>();
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
