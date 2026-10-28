package com.technova.campusdesk.exception;

// Conflicts with a business rule (e.g. forbidden status change). Returns HTTP 409.
public class BusinessRuleException extends RuntimeException {

  public BusinessRuleException(String message) {
    super(message);
  }
}
