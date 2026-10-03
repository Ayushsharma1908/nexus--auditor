package com.nexuscomply.cyber.compliance.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Framework {

    private String id;
    private String code;
    private String name;
    private String version;
    private String category;
    private String description;
    private String status = "ACTIVE";
    private int controlCount;
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public Framework() {}

    public Framework(String id, String code, String name, String version, String category, String description, String status, int controlCount) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.version = version;
        this.category = category;
        this.description = description;
        this.status = status;
        this.controlCount = controlCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getControlCount() {
        return controlCount;
    }

    public void setControlCount(int controlCount) {
        this.controlCount = controlCount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new LinkedHashMap<>();
    }
}
