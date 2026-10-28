package com.technova.campusdesk.ticket.entity;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

// Support incident reported by a user.
// Indexes are created by schema.sql (not duplicated here).
@Entity
@Table(name = "tickets")
public class Ticket {

  // Auto-generated identifier.
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Short problem title.
  @Column(nullable = false, length = 150)
  private String title;

  // Problem details.
  @Column(nullable = false, length = 2000)
  private String description;

  // Type: HARDWARE, SOFTWARE, REDES, ACCESOS or OTROS.
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TicketCategory category;

  // Urgency: BAJA, MEDIA, ALTA or CRITICA.
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TicketPriority priority;

  // Current lifecycle status.
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TicketStatus status;

  // Who reported the ticket (required).
  @ManyToOne(optional = false)
  @JoinColumn(name = "requester_id", nullable = false)
  private User requester;

  // Technician handling it (empty until an ADMIN assigns one).
  @ManyToOne
  @JoinColumn(name = "assigned_technician_id")
  private User technician;

  // Creation and last-update dates, set automatically.
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // SLA deadline: createdAt + hours of the priority.
  @Column(name = "due_at")
  private Instant dueAt;

  // Requester's satisfaction (1-5), only set when closing.
  @Column(name = "rating")
  private Short rating;

  // Optional text that comes with the rating.
  @Column(name = "feedback", length = 500)
  private String feedback;

  // Requester's reason, only set when cancelling.
  @Column(name = "cancel_reason", length = 500)
  private String cancelReason;

  public Ticket() {
    // Empty constructor required by JPA.
  }

  // Sets both dates right before the first save.
  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
    updatedAt = createdAt;
    recalculateDueAt();
  }

  // Recomputes the SLA deadline from the creation date and current priority.
  public void recalculateDueAt() {
    if (createdAt != null && priority != null) {
      dueAt = createdAt.plus(priority.sla());
    }
  }

  // True while the ticket is still being worked on and its deadline passed.
  public boolean isOverdue() {
    return dueAt != null && Instant.now().isAfter(dueAt)
        && status != TicketStatus.RESUELTA && !status.isFinal();
  }

  // Refreshes the date every time the ticket changes.
  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public Long getId() { return id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public TicketCategory getCategory() { return category; }
  public void setCategory(TicketCategory category) { this.category = category; }
  public TicketPriority getPriority() { return priority; }
  public void setPriority(TicketPriority priority) { this.priority = priority; }
  public TicketStatus getStatus() { return status; }
  public void setStatus(TicketStatus status) { this.status = status; }
  public User getRequester() { return requester; }
  public void setRequester(User requester) { this.requester = requester; }
  public User getTechnician() { return technician; }
  public void setTechnician(User technician) { this.technician = technician; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Instant getDueAt() { return dueAt; }
  public Short getRating() { return rating; }
  public void setRating(Short rating) { this.rating = rating; }
  public String getFeedback() { return feedback; }
  public void setFeedback(String feedback) { this.feedback = feedback; }
  public String getCancelReason() { return cancelReason; }
  public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}
