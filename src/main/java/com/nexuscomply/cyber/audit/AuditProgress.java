package com.nexuscomply.cyber.audit;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Progress indicator for audit execution.
 * Field names match schema1.md section 9 exactly ("stage", "percent").
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditProgress {

    private String stage;
    private int percent;

    public AuditProgress() {}

    public AuditProgress(String stage, int percent) {
        this.stage = stage;
        this.percent = percent;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }
}
