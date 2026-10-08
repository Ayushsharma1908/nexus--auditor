package com.nexuscomply.framework.service;

public class DatasetValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DatasetValidationException(String message) {
        super(message);
    }

    public DatasetValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
