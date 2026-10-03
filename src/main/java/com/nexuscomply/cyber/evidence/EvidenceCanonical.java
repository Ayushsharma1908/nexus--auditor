package com.nexuscomply.cyber.evidence;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Canonical field and value associated with an evidence record.
 * Field names match schema1.md section 11 exactly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvidenceCanonical {

    private String field;
    private Object value;

    public EvidenceCanonical() {}

    public EvidenceCanonical(String field, Object value) {
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
