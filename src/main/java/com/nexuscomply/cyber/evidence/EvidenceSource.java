package com.nexuscomply.cyber.evidence;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Technical source location of evidence.
 * Field names match schema1.md section 11 exactly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvidenceSource {

    private Integer lineNumber;
    private String rawText;
    private String sourceType = "CONFIGURATION";

    public EvidenceSource() {}

    public EvidenceSource(Integer lineNumber, String rawText, String sourceType) {
        this.lineNumber = lineNumber;
        this.rawText = rawText;
        this.sourceType = sourceType != null ? sourceType : "CONFIGURATION";
    }

    public EvidenceSource(Integer lineNumber, String rawText) {
        this(lineNumber, rawText, "CONFIGURATION");
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
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
