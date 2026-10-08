package com.nexuscomply.normalization.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexuscomply.normalization.model.NormalizedConfiguration;
import com.nexuscomply.normalization.model.SourceMapEntry;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NormalizedConfigurationResponse {
    private String id;
    private String configurationId;
    private String versionId;
    private String deviceId;
    private String vendor;
    private String platform;
    private Map<String, Object> canonicalState;
    private List<SourceMapEntry> sourceMap;
    private Instant normalizedAt;

    public NormalizedConfigurationResponse() {}

    public static NormalizedConfigurationResponse fromEntity(NormalizedConfiguration entity) {
        NormalizedConfigurationResponse res = new NormalizedConfigurationResponse();
        res.setId(entity.getId());
        res.setConfigurationId(entity.getConfigurationId());
        res.setVersionId(entity.getVersionId());
        res.setDeviceId(entity.getDeviceId());
        res.setVendor(entity.getVendor());
        res.setPlatform(entity.getPlatform());
        res.setCanonicalState(entity.getCanonicalState());
        res.setSourceMap(entity.getSourceMap());
        res.setNormalizedAt(entity.getNormalizedAt());
        return res;
    }

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
        this.canonicalState = canonicalState;
    }

    public List<SourceMapEntry> getSourceMap() {
        return sourceMap;
    }

    public void setSourceMap(List<SourceMapEntry> sourceMap) {
        this.sourceMap = sourceMap;
    }

    public Instant getNormalizedAt() {
        return normalizedAt;
    }

    public void setNormalizedAt(Instant normalizedAt) {
        this.normalizedAt = normalizedAt;
    }
}
