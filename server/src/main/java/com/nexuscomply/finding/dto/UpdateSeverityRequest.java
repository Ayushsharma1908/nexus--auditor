package com.nexuscomply.finding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateSeverityRequest {
    private String severity;
    private String note;

    public UpdateSeverityRequest() {}

    public UpdateSeverityRequest(String severity, String note) {
        this.severity = severity;
        this.note = note;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
