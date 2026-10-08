package com.nexuscomply.cyber.simulation;

/**
 * Sub-document representing a simulated canonical field change (schema1.md section 14).
 */
public class SimulationChange {

    private String canonicalField;
    private Object oldValue;
    private Object newValue;
    private String classification; // IMPROVED, DEGRADED, NO_SECURITY_IMPACT, UNKNOWN_IMPACT

    public SimulationChange() {}

    public SimulationChange(String canonicalField, Object oldValue, Object newValue) {
        this.canonicalField = canonicalField;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public SimulationChange(String canonicalField, Object oldValue, Object newValue, String classification) {
        this.canonicalField = canonicalField;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.classification = classification;
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }
}
