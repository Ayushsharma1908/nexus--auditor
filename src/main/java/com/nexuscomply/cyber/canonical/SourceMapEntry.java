package com.nexuscomply.cyber.canonical;

public class SourceMapEntry {

    private String canonicalField;
    private int sourceLine;
    private String rawText;
    private String sourceType = "CONFIGURATION";

    public SourceMapEntry() {}

    public SourceMapEntry(String canonicalField, int sourceLine, String rawText) {
        this(canonicalField, sourceLine, rawText, "CONFIGURATION");
    }

    public SourceMapEntry(String canonicalField, int sourceLine, String rawText, String sourceType) {
        this.canonicalField = canonicalField;
        this.sourceLine = sourceLine;
        this.rawText = rawText;
        this.sourceType = sourceType != null ? sourceType : "CONFIGURATION";
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    public void setSourceLine(int sourceLine) {
        this.sourceLine = sourceLine;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}
