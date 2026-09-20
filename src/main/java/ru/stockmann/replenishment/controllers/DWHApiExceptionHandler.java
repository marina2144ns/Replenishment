package ru.stockmann.replenishment.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Consistent bad-request response for common DWH request validation. */
@RestControllerAdvice
public class DWHApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadableRequest(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause instanceof IllegalArgumentException && cause.getMessage() != null
                ? cause.getMessage()
                : "Malformed request body";
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
}
