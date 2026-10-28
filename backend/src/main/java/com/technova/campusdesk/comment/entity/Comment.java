package com.technova.campusdesk.comment.entity;

import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

// Comment inside a ticket. Never edited: only created.
// Indexes are created by schema.sql (not duplicated here).
@Entity
@Table(name = "comments")
public class Comment {

  // Auto-generated identifier.
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Comment text.
  @Column(nullable = false, length = 2000)
  private String content;

  // Who wrote it (cannot change afterwards).
  @ManyToOne(optional = false)
  @JoinColumn(name = "author_id", nullable = false, updatable = false)
  private User author;

  // Ticket it belongs to (cannot change afterwards).
  @ManyToOne(optional = false)
  @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
  private Ticket ticket;

  // Internal note: only TECHNICIAN and ADMIN can see it.
  @Column(nullable = false, updatable = false)
  private boolean internal;

  // Creation date, set automatically.
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Comment() {
    // Empty constructor required by JPA.
  }

  public Comment(Ticket ticket, User author, String content) {
    this(ticket, author, content, false);
  }

  public Comment(Ticket ticket, User author, String content, boolean internal) {
    this.ticket = ticket;
    this.author = author;
    this.content = content;
    this.internal = internal;
  }

  // Sets the date right before saving.
  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  public Long getId() { return id; }
  public String getContent() { return content; }
  public User getAuthor() { return author; }
  public Ticket getTicket() { return ticket; }
  public boolean isInternal() { return internal; }
  public Instant getCreatedAt() { return createdAt; }
}
