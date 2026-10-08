package com.nexuscomply.common.exception;

import com.nexuscomply.common.api.ErrorBody;
import com.nexuscomply.common.api.ErrorDetail;
import com.nexuscomply.common.api.ErrorResponse;
import com.nexuscomply.common.security.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        String requestId = RequestIdFilter.getRequestId();
        log.warn("API Exception [{}]: {} - Code: {}", requestId, ex.getMessage(), ex.getCode());
        ErrorResponse response = new ErrorResponse(
                new ErrorBody(ex.getCode(), ex.getMessage(), ex.getDetails()),
                requestId
        );
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String requestId = RequestIdFilter.getRequestId();
        List<ErrorDetail> details = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(new ErrorDetail(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getRejectedValue()
            ));
        }
        log.warn("Validation error [{}]: {} fields rejected", requestId, details.size());
        ErrorResponse response = new ErrorResponse(
                new ErrorBody("VALIDATION_FAILED", "Request validation failed.", details),
                requestId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        String requestId = RequestIdFilter.getRequestId();
        log.warn("Malformed JSON request [{}]: {}", requestId, ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                new ErrorBody("MALFORMED_REQUEST", "Malformed JSON request body.", List.of()),
                requestId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        String requestId = RequestIdFilter.getRequestId();
        ErrorResponse response = new ErrorResponse(
                new ErrorBody("RESOURCE_NOT_FOUND", "The requested path does not exist.", List.of()),
                requestId
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String requestId = RequestIdFilter.getRequestId();
        ErrorResponse response = new ErrorResponse(
                new ErrorBody("METHOD_NOT_ALLOWED", "HTTP method not supported for this endpoint.", List.of()),
                requestId
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        String requestId = RequestIdFilter.getRequestId();
        ErrorResponse response = new ErrorResponse(
                new ErrorBody("FORBIDDEN", "Access is denied for this resource.", List.of()),
                requestId
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        String requestId = RequestIdFilter.getRequestId();
        ErrorResponse response = new ErrorResponse(
                new ErrorBody("UNAUTHORIZED", "Full authentication is required to access this resource.", List.of()),
                requestId
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        String requestId = RequestIdFilter.getRequestId();
        log.error("Unhandled server exception [{}]: ", requestId, ex);
        ErrorResponse response = new ErrorResponse(
                new ErrorBody("INTERNAL_SERVER_ERROR", "An unexpected error occurred. Reference: " + requestId, List.of()),
                requestId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
