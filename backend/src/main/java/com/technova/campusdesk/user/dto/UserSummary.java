package com.technova.campusdesk.user.dto;

import com.technova.campusdesk.user.entity.Role;
import com.technova.campusdesk.user.entity.User;

// Short user view, embedded in tickets and comments.
public record UserSummary(
    Long id,
    String fullName,
    String email,
    Role role) {

  // Converts the entity into a summary.
  public static UserSummary from(User user) {
    return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
  }
}
