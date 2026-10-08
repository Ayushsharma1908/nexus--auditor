package com.nexuscomply.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private String requestId;

    public ApiResponse() {}

    public ApiResponse(T data, String requestId) {
        this.data = data;
        this.requestId = requestId;
    }

    public static <T> ApiResponse<T> of(T data, String requestId) {
        return new ApiResponse<>(data, requestId);
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, "REQ-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
