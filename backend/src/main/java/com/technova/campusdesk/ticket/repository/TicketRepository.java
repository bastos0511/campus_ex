package com.technova.campusdesk.ticket.repository;

import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// Access to the tickets table. Role filters live in TicketSpecifications.
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

  // Tickets in a status whose last change is older than the limit
  // (used by the auto-close job).
  List<Ticket> findByStatusAndUpdatedAtBefore(TicketStatus status, Instant limit);
}
