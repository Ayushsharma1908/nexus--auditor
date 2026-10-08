package com.nexuscomply.remediation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** BUG-003: MongoDB document for Remediation Templates (read-only library). */
@Document(collection = "remediation_templates")
public class RemediationTemplate {

    @Id
    private String id;

    private String title;
    private String description;
    private String category;
    private String controlId;    // non-cyber reference

    public RemediationTemplate() {}

    public String getId()                { return id; }
    public void setId(String id)         { this.id = id; }

    public String getTitle()             { return title; }
    public void setTitle(String title)   { this.title = title; }

    public String getDescription()                  { return description; }
    public void setDescription(String description)  { this.description = description; }

    public String getCategory()                { return category; }
    public void setCategory(String category)   { this.category = category; }

    public String getControlId()               { return controlId; }
    public void setControlId(String controlId) { this.controlId = controlId; }
}
