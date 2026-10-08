package com.nexuscomply.remediation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** BUG-007: Request body for POST /api/v1/findings/{id}/remediation/plan (B-067) */
public class CreateRemediationPlanRequest {

    @NotBlank(message = "Plan title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    private String notes;

    public CreateRemediationPlanRequest() {}

    public String getTitle()             { return title; }
    public void setTitle(String title)   { this.title = title; }

    public String getNotes()             { return notes; }
    public void setNotes(String notes)   { this.notes = notes; }
}
