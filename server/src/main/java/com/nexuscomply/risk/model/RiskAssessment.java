package com.nexuscomply.risk.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.Instant;

/** BUG-003: MongoDB document for Risk Assessments collection. */
@Document(collection = "risk_assessments")
public class RiskAssessment {

    @Id
    private String id;

    private String title;
    private String severity;   // CRITICAL, HIGH, MEDIUM, LOW
    private double score;
    private Instant calculatedAt;

    @Indexed
    private String deviceId;

    @Indexed
    private String findingId;

    @Indexed
    private String auditId;

    public RiskAssessment() {}

    public String getId()               { return id; }
    public void setId(String id)        { this.id = id; }

    public String getTitle()            { return title; }
    public void setTitle(String title)  { this.title = title; }

    public String getSeverity()               { return severity; }
    public void setSeverity(String severity)  { this.severity = severity; }

    public double getScore()            { return score; }
    public void setScore(double score)  { this.score = score; }

    public Instant getCalculatedAt()                  { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }

    public String getDeviceId()               { return deviceId; }
    public void setDeviceId(String deviceId)  { this.deviceId = deviceId; }

    public String getFindingId()                { return findingId; }
    public void setFindingId(String findingId)  { this.findingId = findingId; }

    public String getAuditId()              { return auditId; }
    public void setAuditId(String auditId)  { this.auditId = auditId; }
}
