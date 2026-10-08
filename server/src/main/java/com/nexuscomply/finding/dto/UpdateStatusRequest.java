package com.nexuscomply.finding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateStatusRequest {
    private String status;
    private String note;

    public UpdateStatusRequest() {}

    public UpdateStatusRequest(String status, String note) {
        this.status = status;
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
