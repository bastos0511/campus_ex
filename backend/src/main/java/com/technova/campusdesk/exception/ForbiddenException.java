package com.technova.campusdesk.exception;

// No permission for that operation. Returns HTTP 403.
public class ForbiddenException extends RuntimeException {

  public ForbiddenException(String message) {
    super(message);
  }
}
