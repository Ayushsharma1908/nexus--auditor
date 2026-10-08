package com.nexuscomply.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetail {
    private String field;
    private String message;
    private Object rejectedValue;

    public ErrorDetail() {}

    public ErrorDetail(String message) {
        this.message = message;
    }

    public ErrorDetail(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public ErrorDetail(String field, String message, Object rejectedValue) {
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    public void setRejectedValue(Object rejectedValue) {
        this.rejectedValue = rejectedValue;
    }
}
