package com.technova.campusdesk.user.dto;

import com.technova.campusdesk.user.entity.Role;
import com.technova.campusdesk.user.entity.User;
import java.time.Instant;

// Full user view. Never includes the password.
public record UserResponse(
    Long id,
    String fullName,
    String email,
    Role role,
    Instant createdAt) {

  // Converts the entity into a response.
  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getFullName(), user.getEmail(),
        user.getRole(), user.getCreatedAt());
  }
}
