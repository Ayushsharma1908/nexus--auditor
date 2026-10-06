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
    private int evaluated;
    private double coverage;

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
        this.evaluated = passed + failed + unknown + error;
        this.coverage = totalControls > 0 ? (double) this.evaluated / totalControls : 0.0;
    }

    public int getTotalControls() {
        return totalControls;
    }

    public void setTotalControls(int totalControls) {
        this.totalControls = totalControls;
        this.coverage = totalControls > 0 ? (double) getEvaluated() / totalControls : 0.0;
    }

    public int getPassed() {
        return passed;
    }

    public void setPassed(int passed) {
        this.passed = passed;
        this.evaluated = getEvaluated();
        this.coverage = totalControls > 0 ? (double) this.evaluated / totalControls : 0.0;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
        this.evaluated = getEvaluated();
        this.coverage = totalControls > 0 ? (double) this.evaluated / totalControls : 0.0;
    }

    public int getUnknown() {
        return unknown;
    }

    public void setUnknown(int unknown) {
        this.unknown = unknown;
        this.evaluated = getEvaluated();
        this.coverage = totalControls > 0 ? (double) this.evaluated / totalControls : 0.0;
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
        this.evaluated = getEvaluated();
        this.coverage = totalControls > 0 ? (double) this.evaluated / totalControls : 0.0;
    }

    public int getEvaluated() {
        return passed + failed + unknown + error;
    }

    public void setEvaluated(int evaluated) {
        this.evaluated = evaluated;
    }

    public double getCoverage() {
        return totalControls > 0 ? (double) getEvaluated() / totalControls : 0.0;
    }

    public void setCoverage(double coverage) {
        this.coverage = coverage;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getEvaluatedControls() {
        return getEvaluated();
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public double getCoveragePercentage() {
        return getCoverage() * 100.0;
    }
}
