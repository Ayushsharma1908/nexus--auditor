package com.nexuscomply.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorBody {
    private String code;
    private String message;
    private List<ErrorDetail> details = new ArrayList<>();

    public ErrorBody() {}

    public ErrorBody(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorBody(String code, String message, List<ErrorDetail> details) {
        this.code = code;
        this.message = message;
        this.details = details != null ? details : new ArrayList<>();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ErrorDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ErrorDetail> details) {
        this.details = details;
    }
}
