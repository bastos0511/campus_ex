package com.technova.campusdesk.ticket.dto;

import jakarta.validation.constraints.NotNull;

// Asks to assign a ticket to a technician (by id).
public record AssignRequest(
    @NotNull(message = "Debe indicar el tecnico a asignar")
    Long technicianId) {
}
