package com.nexuscomply.simulation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/** Database contract: MongoDB document for What-If Simulations collection. */
@Document(collection = "what_if_simulations")
public class Simulation {

    @Id
    private String id;

    private String name;
    private String status;  // QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
    private Instant createdAt;
    private Instant updatedAt;

    public Simulation() {}

    public String getId()               { return id; }
    public void setId(String id)        { this.id = id; }

    public String getName()             { return name; }
    public void setName(String name)    { this.name = name; }

    public String getStatus()               { return status; }
    public void setStatus(String status)    { this.status = status; }

    public Instant getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(Instant createdAt)   { this.createdAt = createdAt; }

    public Instant getUpdatedAt()                 { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt)   { this.updatedAt = updatedAt; }
}
