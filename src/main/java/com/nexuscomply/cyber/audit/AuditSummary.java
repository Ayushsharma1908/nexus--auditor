package com.nexuscomply.cyber.audit;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Summary counters for compliance rule evaluation results.
 * Core fields matching schema1.md section 9:
 * totalControls, passed, failed, unknown, notApplicable.
 *
 * Operational extension field:
 * error — tracks rule evaluation runtime exceptions and null evaluation results.
 *
 * Reconciled identity:
 * totalControls = passed + failed + unknown + notApplicable + error.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditSummary {

    private int totalControls;
    private int passed;
    private int failed;
    private int unknown;
    private int notApplicable;
    private int error;

    public AuditSummary() {}

    public AuditSummary(int totalControls, int passed, int failed, int unknown, int notApplicable) {
        this(totalControls, passed, failed, unknown, notApplicable, 0);
    }

    public AuditSummary(int totalControls, int passed, int failed, int unknown, int notApplicable, int error) {
        this.totalControls = totalControls;
        this.passed = passed;
        this.failed = failed;
        this.unknown = unknown;
        this.notApplicable = notApplicable;
        this.error = error;
    }

    public int getTotalControls() {
        return totalControls;
    }

    public void setTotalControls(int totalControls) {
        this.totalControls = totalControls;
    }

    public int getPassed() {
        return passed;
    }

    public void setPassed(int passed) {
        this.passed = passed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getUnknown() {
        return unknown;
    }

    public void setUnknown(int unknown) {
        this.unknown = unknown;
    }

    public int getNotApplicable() {
        return notApplicable;
    }

    public void setNotApplicable(int notApplicable) {
        this.notApplicable = notApplicable;
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }
}
