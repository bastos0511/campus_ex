package com.technova.campusdesk.auth.dto;

import com.technova.campusdesk.user.dto.UserResponse;

// Login response: JWT token + user data.
public record AuthResponse(
    String token,
    String tokenType,
    long expiresInSeconds,
    UserResponse user) {
}
