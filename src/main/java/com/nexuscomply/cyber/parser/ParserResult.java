package com.nexuscomply.cyber.parser;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;

import java.util.ArrayList;
import java.util.List;

public class ParserResult {

    private CanonicalSecurityModel canonical = new CanonicalSecurityModel();
    private List<SourceMapEntry> sourceMap = new ArrayList<>();
    private List<UnknownConstruct> unknowns = new ArrayList<>();
    private String status = "COMPLETED";

    public ParserResult() {}

    public ParserResult(CanonicalSecurityModel canonical, List<SourceMapEntry> sourceMap, List<UnknownConstruct> unknowns, String status) {
        this.canonical = canonical != null ? canonical : new CanonicalSecurityModel();
        this.sourceMap = sourceMap != null ? sourceMap : new ArrayList<>();
        this.unknowns = unknowns != null ? unknowns : new ArrayList<>();
        this.status = status != null ? status : "COMPLETED";
    }

    public CanonicalSecurityModel getCanonical() {
        return canonical;
    }

    public void setCanonical(CanonicalSecurityModel canonical) {
        this.canonical = canonical != null ? canonical : new CanonicalSecurityModel();
    }

    public List<SourceMapEntry> getSourceMap() {
        return sourceMap;
    }

    public void setSourceMap(List<SourceMapEntry> sourceMap) {
        this.sourceMap = sourceMap != null ? sourceMap : new ArrayList<>();
    }

    public List<UnknownConstruct> getUnknowns() {
        return unknowns;
    }

    public void setUnknowns(List<UnknownConstruct> unknowns) {
        this.unknowns = unknowns != null ? unknowns : new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
