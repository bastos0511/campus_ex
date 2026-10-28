package com.technova.campusdesk.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

// System user (requester, technician or administrator).
@Entity
@Table(name = "users")
public class User {

  // Auto-generated identifier.
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Full name.
  @Column(name = "name", nullable = false, length = 100)
  private String fullName;

  // Unique email: used to log in.
  @Column(nullable = false, unique = true, length = 150)
  private String email;

  // BCrypt-hashed password (never plain text).
  @Column(name = "password", nullable = false, length = 100)
  private String passwordHash;

  // Role stored as text (ADMIN, TECHNICIAN, USER).
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  // Creation date, set automatically.
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected User() {
    // Empty constructor required by JPA.
  }

  public User(String fullName, String email, String passwordHash, Role role) {
    this.fullName = fullName;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
  }

  // Sets the date right before the first save.
  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  public Long getId() { return id; }
  public String getFullName() { return fullName; }
  public void setFullName(String fullName) { this.fullName = fullName; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public Role getRole() { return role; }
  public void setRole(Role role) { this.role = role; }
  public Instant getCreatedAt() { return createdAt; }
}
