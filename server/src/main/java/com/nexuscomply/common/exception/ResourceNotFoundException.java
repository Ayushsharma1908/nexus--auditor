package com.nexuscomply.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceType, String id) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", resourceType + " with ID '" + id + "' was not found.");
    }
}
