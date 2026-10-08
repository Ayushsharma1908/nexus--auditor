package com.nexuscomply.finding.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "evidence")
public class Evidence {
    @Id
    private String id;
    private String findingId;
    private String auditId;
    private String deviceId;
    private String configurationId;
    private String versionId;
    private String configurationVersionId;
    private String type = "CONFIGURATION_SNIPPET";
    private String snippet;
    private Integer startLine;
    private Integer endLine;
    private String sourcePath;
    private Instant collectedAt = Instant.now();

    public Evidence() {}

    public Evidence(String id, String findingId, String auditId, String deviceId, String configurationId, String versionId, String snippet, Integer startLine, Integer endLine, String sourcePath) {
        this.id = id;
        this.findingId = findingId;
        this.auditId = auditId;
        this.deviceId = deviceId;
        this.configurationId = configurationId;
        this.versionId = versionId;
        this.snippet = snippet;
        this.startLine = startLine;
        this.endLine = endLine;
        this.sourcePath = sourcePath;
        this.collectedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFindingId() {
        return findingId;
    }

    public void setFindingId(String findingId) {
        this.findingId = findingId;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getVersionId() {
        return versionId != null ? versionId : configurationVersionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
        if (this.configurationVersionId == null) {
            this.configurationVersionId = versionId;
        }
    }

    public String getConfigurationVersionId() {
        return configurationVersionId != null ? configurationVersionId : versionId;
    }

    public void setConfigurationVersionId(String configurationVersionId) {
        this.configurationVersionId = configurationVersionId;
        if (this.versionId == null) {
            this.versionId = configurationVersionId;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }
}
