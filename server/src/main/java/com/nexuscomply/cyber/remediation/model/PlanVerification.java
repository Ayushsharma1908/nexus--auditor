package com.nexuscomply.cyber.remediation.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanVerification {

    private String status = "PENDING";
    private Instant verifiedAt;
    private List<String> messages = new ArrayList<>();

    public PlanVerification() {}

    public PlanVerification(String status, Instant verifiedAt, List<String> messages) {
        this.status = status;
        this.verifiedAt = verifiedAt;
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }
}
