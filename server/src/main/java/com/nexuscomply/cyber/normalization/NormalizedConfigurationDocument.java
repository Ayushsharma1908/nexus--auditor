package com.nexuscomply.cyber.normalization;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.parser.UnknownConstruct;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "normalized_configurations")
@CompoundIndex(name = "device_created_idx", def = "{'deviceId': 1, 'createdAt': -1}")
public class NormalizedConfigurationDocument {

    @Id
    private String id;

    @Indexed
    private String deviceId;

    @Indexed
    private String configurationId;

    @Indexed(unique = true)
    private String versionId;

    private String vendor;
    private String platform;
    private String osVersion;
    private String schemaVersion = "1.0";

    private CanonicalSecurityModel canonical = new CanonicalSecurityModel();
    private List<SourceMapEntry> sourceMap = new ArrayList<>();
    private List<UnknownConstruct> unknowns = new ArrayList<>();

    private String normalizationStatus = "COMPLETED";

    @Indexed
    private Instant createdAt;
    private Instant updatedAt;

    public NormalizedConfigurationDocument() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
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

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public CanonicalSecurityModel getCanonical() {
        return canonical;
    }

    public void setCanonical(CanonicalSecurityModel canonical) {
        this.canonical = canonical;
    }

    public List<SourceMapEntry> getSourceMap() {
        return sourceMap;
    }

    public void setSourceMap(List<SourceMapEntry> sourceMap) {
        this.sourceMap = sourceMap;
    }

    public List<UnknownConstruct> getUnknowns() {
        return unknowns;
    }

    public void setUnknowns(List<UnknownConstruct> unknowns) {
        this.unknowns = unknowns;
    }

    public String getNormalizationStatus() {
        return normalizationStatus;
    }

    public void setNormalizationStatus(String normalizationStatus) {
        this.normalizationStatus = normalizationStatus;
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
