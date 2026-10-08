package com.nexuscomply.normalization.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SourceMapEntry {
    private String factPath;
    private Integer line;
    private String rawSnippet;

    public SourceMapEntry() {}

    public SourceMapEntry(String factPath, Integer line, String rawSnippet) {
        this.factPath = factPath;
        this.line = line;
        this.rawSnippet = rawSnippet;
    }

    public String getFactPath() {
        return factPath;
    }

    public void setFactPath(String factPath) {
        this.factPath = factPath;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public String getRawSnippet() {
        return rawSnippet;
    }

    public void setRawSnippet(String rawSnippet) {
        this.rawSnippet = rawSnippet;
    }
}
