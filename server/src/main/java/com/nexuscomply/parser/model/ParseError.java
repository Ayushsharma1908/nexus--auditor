package com.nexuscomply.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParseError {
    private Integer line;
    private Integer column;
    private String rawLine;
    private String code;
    private String reason;

    public ParseError() {}

    public ParseError(Integer line, String reason) {
        this.line = line;
        this.reason = reason;
    }

    public ParseError(Integer line, Integer column, String rawLine, String code, String reason) {
        this.line = line;
        this.column = column;
        this.rawLine = rawLine;
        this.code = code;
        this.reason = reason;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public Integer getColumn() {
        return column;
    }

    public void setColumn(Integer column) {
        this.column = column;
    }

    public String getRawLine() {
        return rawLine;
    }

    public void setRawLine(String rawLine) {
        this.rawLine = rawLine;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
