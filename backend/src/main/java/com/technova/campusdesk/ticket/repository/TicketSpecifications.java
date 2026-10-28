package com.technova.campusdesk.ticket.repository;

import com.technova.campusdesk.user.entity.Role;
import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.ticket.entity.TicketCategory;
import com.technova.campusdesk.ticket.entity.TicketPriority;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

// Ticket filters translated to SQL.
// ADMIN sees everything; TECHNICIAN only assigned; USER only own.
public final class TicketSpecifications {

  // Private constructor: this class only has static functions.
  private TicketSpecifications() {
  }

  // Limits tickets according to the requester's role.
  public static Specification<Ticket> visibleTo(Long userId, Role role) {
    return (root, query, criteria) -> switch (role) {
      case ADMIN -> criteria.conjunction(); // no filter: sees everything
      case TECHNICIAN -> criteria.equal(root.get("technician").get("id"), userId);
      case USER -> criteria.equal(root.get("requester").get("id"), userId);
    };
  }

  // Filters by status (null means no filter).
  public static Specification<Ticket> hasStatus(TicketStatus status) {
    return (root, query, criteria) ->
        status == null ? criteria.conjunction() : criteria.equal(root.get("status"), status);
  }

  // Filters by priority (null means no filter).
  public static Specification<Ticket> hasPriority(TicketPriority priority) {
    return (root, query, criteria) ->
        priority == null ? criteria.conjunction() : criteria.equal(root.get("priority"), priority);
  }

  // Filters by category (null means no filter).
  public static Specification<Ticket> hasCategory(TicketCategory category) {
    return (root, query, criteria) ->
        category == null ? criteria.conjunction() : criteria.equal(root.get("category"), category);
  }

  // Free-text search on title, description and requester name.
  // "#12" or "12" also matches the ticket id.
  public static Specification<Ticket> textContains(String text) {
    return (root, query, criteria) -> {
      if (text == null || text.isBlank()) {
        return criteria.conjunction();
      }
      String clean = text.trim().toLowerCase();
      String like = "%" + escapeLike(clean) + "%";
      var byTitle = criteria.like(criteria.lower(root.get("title")), like, '\\');
      var byDescription = criteria.like(criteria.lower(root.get("description")), like, '\\');
      var byRequester = criteria.like(
          criteria.lower(root.get("requester").get("fullName")), like, '\\');
      String digits = clean.startsWith("#") ? clean.substring(1) : clean;
      if (digits.matches("\\d{1,18}")) {
        return criteria.or(byTitle, byDescription, byRequester,
            criteria.equal(root.get("id"), Long.parseLong(digits)));
      }
      return criteria.or(byTitle, byDescription, byRequester);
    };
  }

  // Only tickets past their SLA that are still being worked on.
  public static Specification<Ticket> overdue(Boolean onlyOverdue) {
    return (root, query, criteria) -> {
      if (!Boolean.TRUE.equals(onlyOverdue)) {
        return criteria.conjunction();
      }
      return criteria.and(
          criteria.lessThan(root.get("dueAt"), Instant.now()),
          root.get("status").in(List.of(
              TicketStatus.ABIERTA, TicketStatus.ASIGNADA, TicketStatus.EN_PROCESO)));
    };
  }

  // Escapes LIKE wildcards so "%" or "_" typed by the user are literal.
  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
