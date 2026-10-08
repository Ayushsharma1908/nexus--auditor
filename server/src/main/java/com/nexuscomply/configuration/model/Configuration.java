package com.nexuscomply.configuration.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "configurations")
public class Configuration {

    @Id
    private String id;

    @Indexed
    private String deviceId;

    private Integer version = 1;
    private String filename;
    private String uploadedBy;
    private String uploadedAt;
    private String size;
    private String status = "Active";
    private String hash;
    private List<String> lines = new ArrayList<>();

    public Configuration() {}

    public Configuration(String id, String deviceId, Integer version, String filename,
                         String uploadedBy, String uploadedAt, String size, String status,
                         String hash, List<String> lines) {
        this.id = id;
        this.deviceId = deviceId;
        this.version = version;
        this.filename = filename;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
        this.size = size;
        this.status = status;
        this.hash = hash;
        this.lines = lines != null ? lines : new ArrayList<>();
    }

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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(String uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }
}
