package com.technova.campusdesk.security;

import com.technova.campusdesk.user.entity.Role;

// Authenticated user traveling on each request (comes from the DB, not the token).
public record AuthUser(
    Long id,
    String email,
    String fullName,
    Role role) {
}
