package com.technova.campusdesk.ticket.controller;

import com.technova.campusdesk.ticket.dto.AssignRequest;
import com.technova.campusdesk.comment.dto.CommentRequest;
import com.technova.campusdesk.comment.dto.CommentResponse;
import com.technova.campusdesk.common.dto.PageResponse;
import com.technova.campusdesk.history.dto.HistoryResponse;
import com.technova.campusdesk.ticket.dto.StatusChangeRequest;
import com.technova.campusdesk.ticket.dto.TicketRequest;
import com.technova.campusdesk.ticket.dto.TicketResponse;
import com.technova.campusdesk.ticket.entity.TicketCategory;
import com.technova.campusdesk.ticket.entity.TicketPriority;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import com.technova.campusdesk.security.AuthUser;
import com.technova.campusdesk.comment.service.CommentService;
import com.technova.campusdesk.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Ticket operations. The rules live in the services.
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

  private final TicketService ticketService;
  private final CommentService commentService;

  // Spring only injects constructor dependencies.
  public TicketController(TicketService ticketService, CommentService commentService) {
    this.ticketService = ticketService;
    this.commentService = commentService;
  }

  // Lists role-visible tickets, with optional filters.
  @GetMapping
  public List<TicketResponse> list(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor,
      @RequestParam(required = false) TicketStatus status,
      @RequestParam(required = false) TicketPriority priority,
      @RequestParam(required = false) TicketCategory category) {
    return ticketService.list(actor, status, priority, category);
  }

  // Paged search with free text (?q=) and SLA filter (?overdue=true).
  // Same role scope as the list. Example: /api/tickets/search?q=vpn&page=0&size=10
  @GetMapping("/search")
  public PageResponse<TicketResponse> search(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor,
      @RequestParam(required = false) TicketStatus status,
      @RequestParam(required = false) TicketPriority priority,
      @RequestParam(required = false) TicketCategory category,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Boolean overdue,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ticketService.search(actor, status, priority, category, q, overdue, page, size);
  }

  // Ticket detail.
  @GetMapping("/{id}")
  public TicketResponse get(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor,
      @PathVariable Long id) {
    return ticketService.get(id, actor);
  }

  // Creates a ticket (born ABIERTA). USER only.
  @PostMapping
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<TicketResponse> create(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor,
      @Valid @RequestBody TicketRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ticketService.create(request, actor));
  }

  // Edits a ticket (requester only, ABIERTA with no technician).
  @PutMapping("/{id}")
  public TicketResponse update(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor,
      @PathVariable Long id,
      @Valid @RequestBody TicketRequest request) {
    return ticketService.update(id, request, actor);
  }

  // Assigns or reassigns a technician (moves to ASIGNADA). ADMIN only.
  @PatchMapping("/{id}/assign")
  @PreAuthorize("hasRole('ADMIN')")
  public TicketResponse assign(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor,
      @PathVariable Long id,
      @Valid @RequestBody AssignRequest request) {
    return ticketService.assign(id, request, actor);
  }

  // Moves status one step (assigned technician or requester, per target).
  // Closing may include rating/feedback; reopening (RESUELTA -> EN_PROCESO) needs comment.
  @PatchMapping("/{id}/status")
  public TicketResponse changeStatus(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor,
      @PathVariable Long id,
      @Valid @RequestBody StatusChangeRequest request) {
    return ticketService.changeStatus(id, request, actor);
  }

  // Ticket comments, in chronological order.
  @GetMapping("/{id}/comments")
  public List<CommentResponse> listComments(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor,
      @PathVariable Long id) {
    return commentService.list(id, actor);
  }

  // Adds a comment (not on CERRADA tickets). Answers 201.
  // internal=true (TECHNICIAN/ADMIN only) hides it from the requester.
  @PostMapping("/{id}/comments")
  public ResponseEntity<CommentResponse> addComment(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor,
      @PathVariable Long id,
      @Valid @RequestBody CommentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(commentService.add(id, request, actor));
  }

  // Status-change history of the ticket.
  @GetMapping("/{id}/history")
  public List<HistoryResponse> getHistory(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor,
      @PathVariable Long id) {
    return ticketService.getHistory(id, actor);
  }
}
