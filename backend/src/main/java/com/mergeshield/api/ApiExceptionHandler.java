package com.mergeshield.api;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(Map.of(
        "error", "INVALID_SPECIFICATION",
        "message", safe(exception.getMessage())
    ));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException exception) {
    String message = exception.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .orElse("Invalid request. Check that both specifications are provided and no larger than 1 MB.");
    return ResponseEntity.badRequest().body(Map.of(
        "error", "INVALID_REQUEST",
        "message", safe(message)
    ));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest().body(Map.of(
        "error", "MALFORMED_REQUEST",
        "message", "Request body must be valid JSON containing oldSpec and newSpec strings."
    ));
  }

  private String safe(String message) {
    if (message == null || message.isBlank()) return "Invalid input";
    return message.substring(0, Math.min(240, message.length()));
  }
}
