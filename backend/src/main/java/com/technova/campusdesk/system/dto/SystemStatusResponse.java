package com.technova.campusdesk.system.dto;

// Diagnostic snapshot of the backend prerequisites.
// Powers GET /api/system/status (public, no sensitive data).
public record SystemStatusResponse(
    boolean dbReachable,
    boolean schemaReady,
    boolean jwtConfigured,
    long users,
    long tickets) {
}
