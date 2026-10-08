package com.nexuscomply.finding.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FindingHistoryEntry {
    private String action;
    private String user;
    private String note;
    private Instant timestamp = Instant.now();

    public FindingHistoryEntry() {}

    public FindingHistoryEntry(String action, String user, String note) {
        this.action = action;
        this.user = user;
        this.note = note;
        this.timestamp = Instant.now();
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
