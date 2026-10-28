package com.technova.campusdesk.history.entity;

import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import com.technova.campusdesk.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

// Record of a status change. Read-only: no setters.
// Indexes are created by schema.sql (not duplicated here).
@Entity
@Table(name = "ticket_history")
public class StatusHistory {

  // Auto-generated identifier.
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Ticket that changed status (cannot change afterwards).
  @ManyToOne(optional = false)
  @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
  private Ticket ticket;

  // Previous status (empty only when the ticket is created).
  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", length = 20, updatable = false)
  private TicketStatus previousStatus;

  // New status it moved to.
  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 20, updatable = false)
  private TicketStatus newStatus;

  // Who made the change (cannot change afterwards).
  @ManyToOne(optional = false)
  @JoinColumn(name = "changed_by_id", nullable = false, updatable = false)
  private User changedBy;

  // Change date, set automatically.
  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  protected StatusHistory() {
    // Empty constructor required by JPA.
  }

  public StatusHistory(Ticket ticket, TicketStatus previousStatus, TicketStatus newStatus, User changedBy) {
    this.ticket = ticket;
    this.previousStatus = previousStatus;
    this.newStatus = newStatus;
    this.changedBy = changedBy;
  }

  // Sets the date right before saving.
  @PrePersist
  void onCreate() {
    changedAt = Instant.now();
  }

  public Long getId() { return id; }
  public Ticket getTicket() { return ticket; }
  public TicketStatus getPreviousStatus() { return previousStatus; }
  public TicketStatus getNewStatus() { return newStatus; }
  public User getChangedBy() { return changedBy; }
  public Instant getChangedAt() { return changedAt; }
}
