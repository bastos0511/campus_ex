package com.technova.campusdesk.exception;

// Resource that does not exist. Returns HTTP 404.
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
