package com.technova.campusdesk.ticket.dto;

import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.ticket.entity.TicketCategory;
import com.technova.campusdesk.ticket.entity.TicketPriority;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import com.technova.campusdesk.user.dto.UserSummary;
import java.time.Instant;

// Full ticket view (technician may be empty).
// dueAt/overdue: SLA. rating/feedback: only on CERRADA tickets that were rated.
public record TicketResponse(
    Long id,
    String title,
    String description,
    TicketCategory category,
    TicketPriority priority,
    TicketStatus status,
    UserSummary requester,
    UserSummary technician,
    Instant createdAt,
    Instant updatedAt,
    Instant dueAt,
    boolean overdue,
    Integer rating,
    String feedback) {

  // Converts the entity into a response.
  public static TicketResponse from(Ticket ticket) {
    return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(),
        ticket.getCategory(), ticket.getPriority(), ticket.getStatus(),
        UserSummary.from(ticket.getRequester()),
        ticket.getTechnician() == null ? null : UserSummary.from(ticket.getTechnician()),
        ticket.getCreatedAt(), ticket.getUpdatedAt(),
        ticket.getDueAt(), ticket.isOverdue(),
        ticket.getRating() == null ? null : ticket.getRating().intValue(),
        ticket.getFeedback());
  }
}
