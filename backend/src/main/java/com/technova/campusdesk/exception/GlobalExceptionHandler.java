package com.technova.campusdesk.exception;

import com.technova.campusdesk.exception.ApiError;
import java.util.List;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Turns every error into a uniform ApiError JSON. Never shows traces or secrets.
@RestControllerAdvice
public class GlobalExceptionHandler {

  // Missing resource -> 404.
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  // No permission (own or Spring's) -> 403.
  @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
  public ResponseEntity<ApiError> handleForbidden(RuntimeException ex) {
    String message = ex instanceof ForbiddenException ? ex.getMessage()
        : "No tienes permiso para realizar esta operacion";
    return build(HttpStatus.FORBIDDEN, message);
  }

  // Business rule or duplicate -> 409.
  @ExceptionHandler({BusinessRuleException.class, DuplicateResourceException.class,
      DataIntegrityViolationException.class})
  public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage());
  }

  // Invalid business input -> 400.
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  // Badly shaped input (@NotBlank validations, malformed JSON,
  // wrong enums or ids...) -> 400.
  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
      TypeMismatchException.class})
  public ResponseEntity<ApiError> handleInvalidInput(Exception ex) {
    return build(HttpStatus.BAD_REQUEST, "Los datos enviados no son validos");
  }

  // Wrong email or password -> 401 (generic message, hides which one failed).
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
    return build(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
  }

  // Any other error -> generic 500.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado");
  }

  // Builds the response with the given status and message.
  private ResponseEntity<ApiError> build(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(ApiError.of(status, message, null, List.of()));
  }
}
