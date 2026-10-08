package com.nexuscomply.ai.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/** BUG-003: MongoDB document for AI Analysis Jobs. */
@Document(collection = "ai_jobs")
public class AiJob {

    @Id
    private String id;

    private String status;   // QUEUED, RUNNING, COMPLETED, FAILED
    private String result;
    private Instant createdAt;
    private Instant updatedAt;

    public AiJob() {}

    public String getId()               { return id; }
    public void setId(String id)        { this.id = id; }

    public String getStatus()               { return status; }
    public void setStatus(String status)    { this.status = status; }

    public String getResult()               { return result; }
    public void setResult(String result)    { this.result = result; }

    public Instant getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(Instant createdAt)   { this.createdAt = createdAt; }

    public Instant getUpdatedAt()                 { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt)   { this.updatedAt = updatedAt; }
}
