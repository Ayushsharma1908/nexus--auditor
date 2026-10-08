package com.nexuscomply.audit.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FrameworkResult {
    private String frameworkId;
    private String frameworkName;
    private Double score;
    private Integer passed;
    private Integer failed;
    private Integer unknowns;

    public FrameworkResult() {}

    public FrameworkResult(String frameworkId, String frameworkName, Double score, Integer passed, Integer failed, Integer unknowns) {
        this.frameworkId = frameworkId;
        this.frameworkName = frameworkName;
        this.score = score;
        this.passed = passed;
        this.failed = failed;
        this.unknowns = unknowns;
    }

    public String getFrameworkId() {
        return frameworkId;
    }

    public void setFrameworkId(String frameworkId) {
        this.frameworkId = frameworkId;
    }

    public String getFrameworkName() {
        return frameworkName;
    }

    public void setFrameworkName(String frameworkName) {
        this.frameworkName = frameworkName;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getPassed() {
        return passed;
    }

    public void setPassed(Integer passed) {
        this.passed = passed;
    }

    public Integer getFailed() {
        return failed;
    }

    public void setFailed(Integer failed) {
        this.failed = failed;
    }

    public Integer getUnknowns() {
        return unknowns;
    }

    public void setUnknowns(Integer unknowns) {
        this.unknowns = unknowns;
    }
}
