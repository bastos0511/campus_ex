package com.technova.campusdesk.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

// Uniform shape of every API error.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> details) {

  // Creates the error with the current time and the HTTP status text.
  public static ApiError of(HttpStatus status, String message, String path, List<String> details) {
    return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
  }
}
