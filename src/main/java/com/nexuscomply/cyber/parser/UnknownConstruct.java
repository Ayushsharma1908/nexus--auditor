package com.nexuscomply.cyber.parser;

public class UnknownConstruct {

    private String rawText;
    private int sourceLine;
    private String reason;

    public UnknownConstruct() {}

    public UnknownConstruct(String rawText, int sourceLine, String reason) {
        this.rawText = rawText;
        this.sourceLine = sourceLine;
        this.reason = reason;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    public void setSourceLine(int sourceLine) {
        this.sourceLine = sourceLine;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
