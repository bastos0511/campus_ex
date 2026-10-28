package com.technova.campusdesk.ticket.scheduler;

import com.technova.campusdesk.comment.entity.Comment;
import com.technova.campusdesk.comment.repository.CommentRepository;
import com.technova.campusdesk.history.entity.StatusHistory;
import com.technova.campusdesk.history.repository.StatusHistoryRepository;
import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import com.technova.campusdesk.ticket.repository.TicketRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// Closes RESUELTA tickets the requester never closed after N days.
// Runs on a cron (default: every day at 03:00). Each ticket is closed in its
// own transaction, so one failure does not undo the others.
@Component
public class AutoCloseJob {

  private static final Logger log = LoggerFactory.getLogger(AutoCloseJob.class);

  private final TicketRepository tickets;
  private final CommentRepository comments;
  private final StatusHistoryRepository history;
  private final TransactionTemplate transaction;
  private final int days;

  public AutoCloseJob(TicketRepository tickets, CommentRepository comments,
      StatusHistoryRepository history, PlatformTransactionManager transactionManager,
      @Value("${app.tickets.auto-close-days}") int days) {
    this.tickets = tickets;
    this.comments = comments;
    this.history = history;
    this.transaction = new TransactionTemplate(transactionManager);
    this.days = days;
  }

  @Scheduled(cron = "${app.tickets.auto-close-cron}")
  public void closeStaleResolvedTickets() {
    if (days <= 0) {
      return; // auto-close disabled
    }
    // A RESUELTA ticket cannot be edited by anyone, so updated_at is the
    // moment it was resolved.
    Instant limit = Instant.now().minus(Duration.ofDays(days));
    List<Long> ids = tickets.findByStatusAndUpdatedAtBefore(TicketStatus.RESUELTA, limit)
        .stream().map(Ticket::getId).toList();
    int closed = 0;
    for (Long id : ids) {
      try {
        Boolean done = transaction.execute(status -> closeOne(id));
        if (Boolean.TRUE.equals(done)) {
          closed++;
        }
      } catch (RuntimeException ex) {
        log.warn("No se pudo cerrar automaticamente el ticket {}: {}", id, ex.getMessage());
      }
    }
    if (closed > 0) {
      log.info("Cierre automatico: {} ticket(s) cerrados tras {} dias en RESUELTA", closed, days);
    }
  }

  // Closes one ticket. Recorded on behalf of the requester (changed_by_id is
  // required) with a comment saying it was the system.
  private boolean closeOne(Long id) {
    Ticket ticket = tickets.findById(id).orElse(null);
    if (ticket == null || ticket.getStatus() != TicketStatus.RESUELTA) {
      return false; // someone changed it meanwhile
    }
    // 1) Comment first: the trg_comments_block_closed trigger rejects
    //    comments once the ticket is CERRADA.
    comments.saveAndFlush(new Comment(ticket, ticket.getRequester(),
        "Cerrado automaticamente por el sistema: el ticket estuvo " + days
            + " dias en estado RESUELTA sin que el solicitante lo cerrara."));
    // 2) Close and leave the history row.
    ticket.setStatus(TicketStatus.CERRADA);
    tickets.saveAndFlush(ticket);
    history.save(new StatusHistory(ticket, TicketStatus.RESUELTA, TicketStatus.CERRADA,
        ticket.getRequester()));
    return true;
  }
}
