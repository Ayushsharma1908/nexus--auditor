package com.nexuscomply.normalization.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexuscomply.normalization.model.SourceMapEntry;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SourceMapResponse {
    private String id;
    private String configurationId;
    private List<SourceMapEntry> sourceMap;
    private Integer totalMappings;

    public SourceMapResponse() {}

    public SourceMapResponse(String id, String configurationId, List<SourceMapEntry> sourceMap) {
        this.id = id;
        this.configurationId = configurationId;
        this.sourceMap = sourceMap;
        this.totalMappings = sourceMap != null ? sourceMap.size() : 0;
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

    public List<SourceMapEntry> getSourceMap() {
        return sourceMap;
    }

    public void setSourceMap(List<SourceMapEntry> sourceMap) {
        this.sourceMap = sourceMap;
        this.totalMappings = sourceMap != null ? sourceMap.size() : 0;
    }

    public Integer getTotalMappings() {
        return totalMappings;
    }

    public void setTotalMappings(Integer totalMappings) {
        this.totalMappings = totalMappings;
    }
}
