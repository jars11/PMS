package com.pms.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps the existing business-layer exceptions to clean HTTP statuses without
 * touching the controllers: {@code IllegalArgumentException} is a bad request
 * (400) unless it says something does not exist (404), {@code
 * IllegalStateException} is a conflict (409, e.g. room already occupied).
 */
@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> manejarArgumento(IllegalArgumentException e) {
        String msg = e.getMessage() == null ? "solicitud invalida" : e.getMessage();
        HttpStatus status = msg.contains("no existe") || msg.contains("inexistente")
                ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiError(status.value(), msg));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> manejarConflicto(IllegalStateException e) {
        String msg = e.getMessage() == null ? "conflicto" : e.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(409, msg));
    }

    /** Simple JSON error shape: code + message. */
    public record ApiError(int code, String mensaje) {
    }
}
