package com.technova.campusdesk.exception;

// Duplicate resource (e.g. email already registered). Returns HTTP 409.
public class DuplicateResourceException extends RuntimeException {

  public DuplicateResourceException(String message) {
    super(message);
  }
}
