package com.nexuscomply.cyber.compliance;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.mongodb.core.mapping.Field;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleRequirement {

    @Field("field")
    @JsonProperty("field")
    @JsonAlias({"field", "canonicalField"})
    private String canonicalField;

    @Field("operator")
    @JsonProperty("operator")
    private String operator;

    @Field("value")
    @JsonProperty("value")
    @JsonAlias({"value", "expectedValue"})
    private Object expectedValue;

    public RuleRequirement() {}

    public RuleRequirement(String canonicalField, String operator, Object expectedValue) {
        this.canonicalField = canonicalField;
        this.operator = operator;
        this.expectedValue = expectedValue;
    }

    @JsonProperty("field")
    public String getCanonicalField() {
        return canonicalField;
    }

    @JsonProperty("field")
    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public String getField() {
        return canonicalField;
    }

    public void setField(String field) {
        this.canonicalField = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @JsonProperty("value")
    public Object getExpectedValue() {
        return expectedValue;
    }

    @JsonProperty("value")
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

