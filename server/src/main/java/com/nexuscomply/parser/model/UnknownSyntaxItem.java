package com.nexuscomply.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnknownSyntaxItem {
    private Integer line;
    private String command;
    private String context;
    private String category;
    private String reason;

    public UnknownSyntaxItem() {}

    public UnknownSyntaxItem(Integer line, String command, String context, String category, String reason) {
        this.line = line;
        this.command = command;
        this.context = context;
        this.category = category;
        this.reason = reason;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
