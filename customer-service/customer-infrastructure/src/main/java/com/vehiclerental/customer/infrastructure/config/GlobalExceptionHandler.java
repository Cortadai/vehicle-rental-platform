package com.vehiclerental.customer.infrastructure.config;

import com.vehiclerental.customer.application.exception.CustomerNotFoundException;
import com.vehiclerental.customer.domain.exception.CustomerDomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(CustomerNotFoundException ex) {
        var body = errorBody(HttpStatus.NOT_FOUND, ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(CustomerDomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomainException(CustomerDomainException ex) {
        var body = errorBody(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> fieldError = new LinkedHashMap<>();
                    fieldError.put("field", error.getField());
                    fieldError.put("message", error.getDefaultMessage());
                    return fieldError;
                })
                .toList();

        var body = new LinkedHashMap<String, Object>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("errors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        var body = errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> errorBody(HttpStatus status, String message, String errorCode) {
        var body = new LinkedHashMap<String, Object>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (errorCode != null) {
            body.put("errorCode", errorCode);
        }
        return body;
    }
}
