package com.nexuscomply.normalization.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "normalized_configurations")
public class NormalizedConfiguration {
    @Id
    private String id;
    private String configurationId;
    private String versionId;
    private String deviceId;
    private String vendor;
    private String platform;
    private Map<String, Object> canonicalState = new HashMap<>();
    private List<SourceMapEntry> sourceMap = new ArrayList<>();
    private Instant normalizedAt = Instant.now();

    public NormalizedConfiguration() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Map<String, Object> getCanonicalState() {
        return canonicalState;
    }

    public void setCanonicalState(Map<String, Object> canonicalState) {
        this.canonicalState = canonicalState != null ? canonicalState : new HashMap<>();
    }

    public List<SourceMapEntry> getSourceMap() {
        return sourceMap;
    }

    public void setSourceMap(List<SourceMapEntry> sourceMap) {
        this.sourceMap = sourceMap != null ? sourceMap : new ArrayList<>();
    }

    public Instant getNormalizedAt() {
        return normalizedAt;
    }

    public void setNormalizedAt(Instant normalizedAt) {
        this.normalizedAt = normalizedAt;
    }
}
