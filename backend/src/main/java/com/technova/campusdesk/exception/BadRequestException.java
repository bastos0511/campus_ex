package com.technova.campusdesk.exception;

// Invalid business input (e.g. assigning to a non-technician). Returns HTTP 400.
public class BadRequestException extends RuntimeException {

  public BadRequestException(String message) {
    super(message);
  }
}
