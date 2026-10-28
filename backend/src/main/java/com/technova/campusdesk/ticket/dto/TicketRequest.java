package com.technova.campusdesk.ticket.dto;

import com.technova.campusdesk.ticket.entity.TicketCategory;
import com.technova.campusdesk.ticket.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Data for creating or editing a ticket. No status or requester: the backend sets them.
public record TicketRequest(
    @NotBlank(message = "El titulo es obligatorio")
    @Size(min = 5, max = 150, message = "El titulo debe tener entre 5 y 150 caracteres")
    String title,

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(min = 10, max = 2000, message = "La descripcion debe tener entre 10 y 2000 caracteres")
    String description,

    @NotNull(message = "La categoria es obligatoria")
    TicketCategory category,

    @NotNull(message = "La prioridad es obligatoria")
    TicketPriority priority) {
}
