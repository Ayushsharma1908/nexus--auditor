package com.nexuscomply.compliance.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnknownComplianceItem {
    private String controlId;
    private String ruleId;
    private String reason;
    private String unresolvedFact;

    public UnknownComplianceItem() {}

    public UnknownComplianceItem(String controlId, String ruleId, String reason, String unresolvedFact) {
        this.controlId = controlId;
        this.ruleId = ruleId;
        this.reason = reason;
        this.unresolvedFact = unresolvedFact;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getUnresolvedFact() {
        return unresolvedFact;
    }

    public void setUnresolvedFact(String unresolvedFact) {
        this.unresolvedFact = unresolvedFact;
    }
}
