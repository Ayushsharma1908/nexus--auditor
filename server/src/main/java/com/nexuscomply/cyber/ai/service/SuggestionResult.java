package com.nexuscomply.cyber.ai.service;

/**
 * Output proposal from an AI {@link SuggestionProvider}.
 */
public class SuggestionResult {

    private String canonicalField;
    private Object mappedValue;
    private String unit;
    private Double confidence;
    private String rationale;

    public SuggestionResult() {}

    public SuggestionResult(String canonicalField, Object mappedValue, Double confidence, String rationale) {
        this(canonicalField, mappedValue, null, confidence, rationale);
    }

    public SuggestionResult(String canonicalField, Object mappedValue, String unit, Double confidence, String rationale) {
        this.canonicalField = canonicalField;
        this.mappedValue = mappedValue;
        this.unit = unit;
        this.confidence = confidence;
        this.rationale = rationale;
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public Object getMappedValue() {
        return mappedValue;
    }

    public void setMappedValue(Object mappedValue) {
        this.mappedValue = mappedValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
