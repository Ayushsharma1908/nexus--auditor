package com.nexuscomply.cyber.remediation.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanValidation {

    private String status = "PENDING";
    private Instant validatedAt;
    private String validatedBy;
    private List<String> messages = new ArrayList<>();

    public PlanValidation() {}

    public PlanValidation(String status, Instant validatedAt, String validatedBy, List<String> messages) {
        this.status = status;
        this.validatedAt = validatedAt;
        this.validatedBy = validatedBy;
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public String getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(String validatedBy) {
        this.validatedBy = validatedBy;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }
}
