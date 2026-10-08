package com.nexuscomply.remediation.dto;

import java.util.UUID;

public class RemediationDTO {
    private UUID id;
    private String title;
    private String planStatus;

    public RemediationDTO() {}

    public RemediationDTO(UUID id, String title, String planStatus) {
        this.id = id;
        this.title = title;
        this.planStatus = planStatus;
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

    public String getPlanStatus() {
        return planStatus;
    }

    public void setPlanStatus(String planStatus) {
        this.planStatus = planStatus;
    }
}
