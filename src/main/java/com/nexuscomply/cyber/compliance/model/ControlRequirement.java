package com.nexuscomply.cyber.compliance.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Denormalized, descriptive requirement structure for the controls collection
 * matching schema1.md Section 7 literally (field, operator, expectedValue).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ControlRequirement {

    @Field("field")
    @JsonProperty("field")
    @JsonAlias({"field", "canonicalField"})
    private String field;

    @Field("operator")
    @JsonProperty("operator")
    private String operator;

    @Field("expectedValue")
    @JsonProperty("expectedValue")
    @JsonAlias({"expectedValue", "value"})
    private Object expectedValue;

    public ControlRequirement() {}

    public ControlRequirement(String field, String operator, Object expectedValue) {
        this.field = field;
        this.operator = operator;
        this.expectedValue = expectedValue;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getCanonicalField() {
        return field;
    }

    public void setCanonicalField(String canonicalField) {
        this.field = canonicalField;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(Object expectedValue) {
        this.expectedValue = expectedValue;
    }

    public Object getValue() {
        return expectedValue;
    }

    public void setValue(Object value) {
        this.expectedValue = value;
    }
}
