package com.technova.campusdesk.ticket.dto;

import com.technova.campusdesk.ticket.entity.TicketStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Asks to move a ticket to a new status.
// rating/feedback: optional, only when closing (CERRADA).
// comment: reason; required when the requester reopens (RESUELTA -> EN_PROCESO).
public record StatusChangeRequest(
    @NotNull(message = "Debe indicar el nuevo estado")
    TicketStatus status,

    @Min(value = 1, message = "La calificacion minima es 1")
    @Max(value = 5, message = "La calificacion maxima es 5")
    Integer rating,

    @Size(max = 500, message = "El comentario de la calificacion admite maximo 500 caracteres")
    String feedback,

    @Size(max = 2000, message = "El motivo es muy largo")
    String comment) {
}
