package com.technova.campusdesk.system.dto;

// Outcome of POST /api/system/seed. Never destructive: when the database
// already holds users, nothing is inserted and seeded is false.
public record SeedResultResponse(
    boolean seeded,
    String message,
    long users,
    long tickets) {
}
