package com.nexuscomply.cyber.compliance;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleEvaluationResult {

    private RuleResultStatus status;
    private String controlId;
    private String ruleId;
    private Object expected;
    private Object actual;
    private String severity;
    private String evidenceSourceField;
    private String message;

    public RuleEvaluationResult() {}

    public RuleEvaluationResult(RuleResultStatus status, String controlId, String ruleId, Object expected, Object actual, String severity, String evidenceSourceField, String message) {
        this.status = status;
        this.controlId = controlId;
        this.ruleId = ruleId;
        this.expected = expected;
        this.actual = actual;
        this.severity = severity;
        this.evidenceSourceField = evidenceSourceField;
        this.message = message;
    }

    public static RuleEvaluationResult pass(String controlId, String ruleId, Object expected, Object actual, String severity, String evidenceSourceField) {
        return new RuleEvaluationResult(RuleResultStatus.PASS, controlId, ruleId, expected, actual, severity, evidenceSourceField, "Requirement satisfied");
    }

    public static RuleEvaluationResult fail(String controlId, String ruleId, Object expected, Object actual, String severity, String evidenceSourceField, String message) {
        return new RuleEvaluationResult(RuleResultStatus.FAIL, controlId, ruleId, expected, actual, severity, evidenceSourceField, message);
    }

    public static RuleEvaluationResult unknown(String controlId, String ruleId, Object expected, String severity, String evidenceSourceField, String message) {
        return new RuleEvaluationResult(RuleResultStatus.UNKNOWN, controlId, ruleId, expected, null, severity, evidenceSourceField, message);
    }

    public static RuleEvaluationResult notApplicable(String controlId, String ruleId, String severity, String message) {
        return new RuleEvaluationResult(RuleResultStatus.NOT_APPLICABLE, controlId, ruleId, null, null, severity, null, message);
    }

    public static RuleEvaluationResult error(String controlId, String ruleId, String severity, String message) {
        return new RuleEvaluationResult(RuleResultStatus.ERROR, controlId, ruleId, null, null, severity, null, message);
    }

    public RuleResultStatus getStatus() {
        return status;
    }

    public void setStatus(RuleResultStatus status) {
        this.status = status;
    }

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Object getExpected() {
        return expected;
    }

    public void setExpected(Object expected) {
        this.expected = expected;
    }

    public Object getActual() {
        return actual;
    }

    public void setActual(Object actual) {
        this.actual = actual;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getEvidenceSourceField() {
        return evidenceSourceField;
    }

    public void setEvidenceSourceField(String evidenceSourceField) {
        this.evidenceSourceField = evidenceSourceField;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
