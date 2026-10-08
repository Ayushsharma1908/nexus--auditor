package com.nexuscomply.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private ErrorBody error;
    private String requestId;

    public ErrorResponse() {}

    public ErrorResponse(ErrorBody error, String requestId) {
        this.error = error;
        this.requestId = requestId;
    }

    public static ErrorResponse of(String code, String message, String requestId) {
        return new ErrorResponse(new ErrorBody(code, message), requestId);
    }

    public ErrorBody getError() {
        return error;
    }

    public void setError(ErrorBody error) {
        this.error = error;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
