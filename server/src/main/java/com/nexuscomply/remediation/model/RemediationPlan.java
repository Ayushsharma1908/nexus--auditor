package com.nexuscomply.remediation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.Instant;

/** BUG-003: MongoDB document for Remediation Plans. */
@Document(collection = "remediation_plans")
public class RemediationPlan {

    @Id
    private String id;

    @Indexed
    private String findingId;

    private String title;
    private String planStatus;   // DRAFT, VALIDATED, VERIFIED, REJECTED
    private Instant createdAt;
    private Instant updatedAt;

    public RemediationPlan() {}

    public String getId()               { return id; }
    public void setId(String id)        { this.id = id; }

    public String getFindingId()                { return findingId; }
    public void setFindingId(String findingId)  { this.findingId = findingId; }

    public String getTitle()             { return title; }
    public void setTitle(String title)   { this.title = title; }

    public String getPlanStatus()                   { return planStatus; }
    public void setPlanStatus(String planStatus)    { this.planStatus = planStatus; }

    public Instant getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(Instant createdAt)   { this.createdAt = createdAt; }

    public Instant getUpdatedAt()                 { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt)   { this.updatedAt = updatedAt; }
}
