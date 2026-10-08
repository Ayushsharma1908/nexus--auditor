package com.nexuscomply.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** BUG-007: Request body for POST /api/v1/reports (B-087) */
public class CreateReportRequest {

    @NotBlank(message = "Report name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    private String name;

    private String auditId;
    private String format;  // PDF, CSV, JSON

    public CreateReportRequest() {}

    public String getName()             { return name; }
    public void setName(String name)    { this.name = name; }

    public String getAuditId()               { return auditId; }
    public void setAuditId(String auditId)   { this.auditId = auditId; }

    public String getFormat()               { return format; }
    public void setFormat(String format)    { this.format = format; }
}
