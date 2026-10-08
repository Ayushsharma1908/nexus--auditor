package com.nexuscomply.risk.dto;

import java.util.UUID;
import java.time.Instant;

public class RiskDTO {
    private UUID id;
    private String title;
    private String severity;
    private double score;
    private Instant calculatedAt;

    public RiskDTO() {}

    public RiskDTO(UUID id, String title, String severity, double score, Instant calculatedAt) {
        this.id = id;
        this.title = title;
        this.severity = severity;
        this.score = score;
        this.calculatedAt = calculatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
