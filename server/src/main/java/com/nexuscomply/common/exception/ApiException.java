package com.nexuscomply.common.exception;

import com.nexuscomply.common.api.ErrorDetail;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final HttpStatus status;
    private final String code;
    private final List<ErrorDetail> details;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = new ArrayList<>();
    }

    public ApiException(HttpStatus status, String code, String message, List<ErrorDetail> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details != null ? details : new ArrayList<>();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<ErrorDetail> getDetails() {
        return details;
    }
}
