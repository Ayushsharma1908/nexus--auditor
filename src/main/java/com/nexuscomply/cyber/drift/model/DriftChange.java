package com.nexuscomply.cyber.drift.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DriftChange {

    private String canonicalField;
    private Object before;
    private Object after;
    private String changeType; // "MODIFIED", "ADDED", "REMOVED"
    private String sourceBefore;
    private String sourceAfter;
    private String classification; // "IMPROVED", "DEGRADED", "NO_SECURITY_IMPACT", "UNKNOWN_IMPACT"

    public DriftChange() {}

    public DriftChange(
            String canonicalField,
            Object before,
            Object after,
            String changeType,
            String sourceBefore,
            String sourceAfter,
            String classification) {
        this.canonicalField = canonicalField;
        this.before = before;
        this.after = after;
        this.changeType = changeType;
        this.sourceBefore = sourceBefore;
        this.sourceAfter = sourceAfter;
        this.classification = classification;
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public Object getBefore() {
        return before;
    }

    public void setBefore(Object before) {
        this.before = before;
    }

    public Object getAfter() {
        return after;
    }

    public void setAfter(Object after) {
        this.after = after;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getSourceBefore() {
        return sourceBefore;
    }

    public void setSourceBefore(String sourceBefore) {
        this.sourceBefore = sourceBefore;
    }

    public String getSourceAfter() {
        return sourceAfter;
    }

    public void setSourceAfter(String sourceAfter) {
        this.sourceAfter = sourceAfter;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }
}
