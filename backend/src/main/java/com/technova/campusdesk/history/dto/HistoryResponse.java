package com.technova.campusdesk.history.dto;

import com.technova.campusdesk.history.entity.StatusHistory;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import com.technova.campusdesk.user.dto.UserSummary;
import java.time.Instant;

// Status-change view with who made it.
public record HistoryResponse(
    Long id,
    Long ticketId,
    TicketStatus previousStatus,
    TicketStatus newStatus,
    UserSummary changedBy,
    Instant changedAt) {

  // Converts the entity into a response.
  public static HistoryResponse from(StatusHistory history) {
    return new HistoryResponse(history.getId(), history.getTicket().getId(),
        history.getPreviousStatus(), history.getNewStatus(),
        UserSummary.from(history.getChangedBy()), history.getChangedAt());
  }
}
