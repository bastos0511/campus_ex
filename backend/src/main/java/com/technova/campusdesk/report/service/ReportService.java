package com.technova.campusdesk.report.service;

import com.technova.campusdesk.report.dto.SummaryResponse;
import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import com.technova.campusdesk.ticket.repository.TicketRepository;
import com.technova.campusdesk.ticket.repository.TicketSpecifications;
import com.technova.campusdesk.security.AuthUser;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Dashboard numbers, counted in the database.
@Service
public class ReportService {

  private final TicketRepository tickets;

  // Spring only injects constructor dependencies.
  public ReportService(TicketRepository tickets) {
    this.tickets = tickets;
  }

  // ADMIN sees system totals; other roles only their allowed tickets.
  @Transactional(readOnly = true)
  public SummaryResponse summary(AuthUser actor) {
    Specification<Ticket> scope = TicketSpecifications.visibleTo(actor.id(), actor.role());
    return new SummaryResponse(
        tickets.count(scope),
        countByStatus(scope, TicketStatus.ABIERTA),
        countByStatus(scope, TicketStatus.ASIGNADA),
        countByStatus(scope, TicketStatus.EN_PROCESO),
        countByStatus(scope, TicketStatus.RESUELTA),
        countByStatus(scope, TicketStatus.CERRADA));
  }

  // Counts in-scope tickets sitting in one status.
  private long countByStatus(Specification<Ticket> scope, TicketStatus status) {
    return tickets.count(scope.and(TicketSpecifications.hasStatus(status)));
  }
}
