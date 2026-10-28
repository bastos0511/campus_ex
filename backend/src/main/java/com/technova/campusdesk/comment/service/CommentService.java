package com.technova.campusdesk.comment.service;

import com.technova.campusdesk.comment.dto.CommentRequest;
import com.technova.campusdesk.comment.dto.CommentResponse;
import com.technova.campusdesk.comment.entity.Comment;
import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import com.technova.campusdesk.user.entity.User;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.user.entity.Role;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.comment.repository.CommentRepository;
import com.technova.campusdesk.ticket.service.TicketService;
import com.technova.campusdesk.user.repository.UserRepository;
import com.technova.campusdesk.security.AuthUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Ticket comments.
@Service
public class CommentService {

  private final CommentRepository comments;
  private final UserRepository users;
  private final TicketService ticketService;

  // Spring only injects constructor dependencies.
  public CommentService(CommentRepository comments, UserRepository users,
      TicketService ticketService) {
    this.comments = comments;
    this.users = users;
    this.ticketService = ticketService;
  }

  // Lists comments in chronological order (only with ticket access).
  // A USER never receives internal notes.
  @Transactional(readOnly = true)
  public List<CommentResponse> list(Long ticketId, AuthUser actor) {
    ticketService.findAccessibleTicket(ticketId, actor);
    List<Comment> rows = actor.role() == Role.USER
        ? comments.findByTicketIdAndInternalFalseOrderByCreatedAtAscIdAsc(ticketId)
        : comments.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId);
    return rows.stream().map(CommentResponse::from).toList();
  }

  // Adds a comment with the authenticated user as author.
  // A CERRADA ticket accepts no comments. Only staff may write internal notes.
  @Transactional
  public CommentResponse add(Long ticketId, CommentRequest request, AuthUser actor) {
    Ticket ticket = ticketService.findAccessibleTicket(ticketId, actor);
    boolean internal = Boolean.TRUE.equals(request.internal());
    if (internal && actor.role() == Role.USER) {
      throw new ForbiddenException("Solo tecnicos y administradores pueden crear notas internas");
    }
    if (ticket.getStatus() == TicketStatus.CERRADA) {
      throw new BusinessRuleException("Un ticket CERRADA no admite comentarios");
    }
    User author = users.findById(actor.id())
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    Comment saved = comments.save(new Comment(ticket, author, request.content().trim(), internal));
    return CommentResponse.from(saved);
  }
}
