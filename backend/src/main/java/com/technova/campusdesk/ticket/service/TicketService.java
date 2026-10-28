package com.technova.campusdesk.ticket.service;

import com.technova.campusdesk.comment.entity.Comment;
import com.technova.campusdesk.comment.repository.CommentRepository;
import com.technova.campusdesk.common.dto.PageResponse;
import com.technova.campusdesk.ticket.dto.AssignRequest;
import com.technova.campusdesk.history.dto.HistoryResponse;
import com.technova.campusdesk.ticket.dto.StatusChangeRequest;
import com.technova.campusdesk.ticket.dto.TicketRequest;
import com.technova.campusdesk.ticket.dto.TicketResponse;
import com.technova.campusdesk.user.entity.Role;
import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.ticket.entity.TicketCategory;
import com.technova.campusdesk.ticket.entity.TicketPriority;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import com.technova.campusdesk.user.entity.User;
import com.technova.campusdesk.exception.BadRequestException;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.history.entity.StatusHistory;
import com.technova.campusdesk.history.repository.StatusHistoryRepository;
import com.technova.campusdesk.ticket.repository.TicketRepository;
import com.technova.campusdesk.ticket.repository.TicketSpecifications;
import com.technova.campusdesk.user.repository.UserRepository;
import com.technova.campusdesk.security.AuthUser;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Ticket business rules: role permissions and controlled lifecycle.
// Each status change and its history record are saved in one transaction.
@Service
public class TicketService {

  // Single allowed step out of each status. CERRADA and CANCELADA are final.
  // ABIERTA -> ASIGNADA only happens via assign(), never via changeStatus().
  // CANCELADA is handled apart: allowed from ABIERTA or ASIGNADA.
  private static final Map<TicketStatus, TicketStatus> NEXT_STATUS = Map.of(
      TicketStatus.ABIERTA, TicketStatus.ASIGNADA,
      TicketStatus.ASIGNADA, TicketStatus.EN_PROCESO,
      TicketStatus.EN_PROCESO, TicketStatus.RESUELTA,
      TicketStatus.RESUELTA, TicketStatus.CERRADA);

  private final TicketRepository tickets;
  private final UserRepository users;
  private final StatusHistoryRepository history;
  private final CommentRepository comments;

  // Largest page the search endpoint will return.
  private static final int MAX_PAGE_SIZE = 50;

  // Length limits of the cancel reason (cancel_reason column and its CHECK).
  private static final int MIN_CANCEL_REASON = 5;
  private static final int MAX_CANCEL_REASON = 500;

  // Spring only injects constructor dependencies.
  public TicketService(TicketRepository tickets, UserRepository users,
      StatusHistoryRepository history, CommentRepository comments) {
    this.tickets = tickets;
    this.users = users;
    this.history = history;
    this.comments = comments;
  }

  // Lists tickets visible by role, with optional filters.
  @Transactional(readOnly = true)
  public List<TicketResponse> list(AuthUser actor, TicketStatus status,
      TicketPriority priority, TicketCategory category) {
    Specification<Ticket> filter = TicketSpecifications.visibleTo(actor.id(), actor.role())
        .and(TicketSpecifications.hasStatus(status))
        .and(TicketSpecifications.hasPriority(priority))
        .and(TicketSpecifications.hasCategory(category));
    Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt", "id");
    return tickets.findAll(filter, newestFirst).stream().map(TicketResponse::from).toList();
  }

  // Paged search: same role scope and filters as list(), plus free text and
  // "only overdue". Newest first; size is capped at MAX_PAGE_SIZE.
  @Transactional(readOnly = true)
  public PageResponse<TicketResponse> search(AuthUser actor, TicketStatus status,
      TicketPriority priority, TicketCategory category, String text, Boolean overdue,
      int page, int size) {
    Specification<Ticket> filter = TicketSpecifications.visibleTo(actor.id(), actor.role())
        .and(TicketSpecifications.hasStatus(status))
        .and(TicketSpecifications.hasPriority(priority))
        .and(TicketSpecifications.hasCategory(category))
        .and(TicketSpecifications.textContains(text))
        .and(TicketSpecifications.overdue(overdue));
    PageRequest pageRequest = PageRequest.of(Math.max(page, 0),
        Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
        Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    return PageResponse.from(tickets.findAll(filter, pageRequest).map(TicketResponse::from));
  }

  // Ticket detail (only with access).
  @Transactional(readOnly = true)
  public TicketResponse get(Long ticketId, AuthUser actor) {
    return TicketResponse.from(findAccessibleTicket(ticketId, actor));
  }

  // Ticket history, oldest change first.
  @Transactional(readOnly = true)
  public List<HistoryResponse> getHistory(Long ticketId, AuthUser actor) {
    findAccessibleTicket(ticketId, actor);
    return history.findByTicketIdOrderByChangedAtAscIdAsc(ticketId)
        .stream().map(HistoryResponse::from).toList();
  }

  // Creates a ticket. USER only; born ABIERTA with no technician.
  @Transactional
  public TicketResponse create(TicketRequest request, AuthUser actor) {
    if (actor.role() != Role.USER) {
      throw new ForbiddenException("Solo los usuarios pueden crear solicitudes de soporte");
    }
    User requester = loadUser(actor.id());
    Ticket ticket = new Ticket();
    applyRequest(ticket, request);
    ticket.setRequester(requester);
    ticket.setStatus(TicketStatus.ABIERTA);
    Ticket saved = tickets.save(ticket);
    recordHistory(saved, null, TicketStatus.ABIERTA, requester);
    return TicketResponse.from(saved);
  }

  // Edits a ticket. Only the requester, while still ABIERTA with no technician.
  @Transactional
  public TicketResponse update(Long ticketId, TicketRequest request, AuthUser actor) {
    Ticket ticket = findAccessibleTicket(ticketId, actor);
    if (actor.role() != Role.USER || !isRequester(ticket, actor)) {
      throw new ForbiddenException("Solo el solicitante puede modificar su ticket");
    }
    if (ticket.getStatus() != TicketStatus.ABIERTA || ticket.getTechnician() != null) {
      throw new BusinessRuleException("Solo se puede modificar un ticket abierto y sin asignar");
    }
    applyRequest(ticket, request);
    ticket.recalculateDueAt(); // the priority may have changed the SLA
    return TicketResponse.from(tickets.saveAndFlush(ticket));
  }

  // Assigns or reassigns a technician. ADMIN only, on ABIERTA or ASIGNADA tickets.
  @Transactional
  public TicketResponse assign(Long ticketId, AssignRequest request, AuthUser actor) {
    if (actor.role() != Role.ADMIN) {
      throw new ForbiddenException("Solo un administrador puede asignar tecnicos");
    }
    Ticket ticket = tickets.findById(ticketId)
        .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));
    TicketStatus current = ticket.getStatus();
    if (current != TicketStatus.ABIERTA && current != TicketStatus.ASIGNADA) {
      throw new BusinessRuleException("Solo se puede asignar o reasignar un ticket ABIERTA o ASIGNADA");
    }
    User technician = users.findById(request.technicianId())
        .orElseThrow(() -> new ResourceNotFoundException("Tecnico no encontrado"));
    if (technician.getRole() != Role.TECHNICIAN) {
      throw new BadRequestException("El usuario seleccionado no es un tecnico");
    }
    // Null-safe: if the invariant (ASIGNADA => with technician) ever broke,
    // reassign instead of crashing with an internal error.
    if (current == TicketStatus.ASIGNADA && ticket.getTechnician() != null
        && Objects.equals(ticket.getTechnician().getId(), technician.getId())) {
      throw new BusinessRuleException("El ticket ya esta asignado a ese tecnico");
    }
    // Reassigning keeps ASIGNADA status and also leaves history.
    ticket.setTechnician(technician);
    ticket.setStatus(TicketStatus.ASIGNADA);
    Ticket saved = tickets.saveAndFlush(ticket);
    recordHistory(saved, current, TicketStatus.ASIGNADA, loadUser(actor.id()));
    return TicketResponse.from(saved);
  }

  // Moves status one step. First checks who may do it (403), then the step (409).
  // Special case: the requester may reopen a RESUELTA ticket (back to EN_PROCESO)
  // giving a reason, which is saved as a comment.
  // Special case: the requester may cancel an ABIERTA or ASIGNADA ticket
  // giving a reason, which is saved in cancel_reason.
  @Transactional
  public TicketResponse changeStatus(Long ticketId, StatusChangeRequest request, AuthUser actor) {
    Ticket ticket = findAccessibleTicket(ticketId, actor);
    TicketStatus target = request.status();
    TicketStatus current = ticket.getStatus();
    boolean reopen = current == TicketStatus.RESUELTA && target == TicketStatus.EN_PROCESO;
    boolean cancel = target == TicketStatus.CANCELADA;
    // Rating only makes sense when closing.
    if (target != TicketStatus.CERRADA && (request.rating() != null || hasText(request.feedback()))) {
      throw new BadRequestException("La calificacion solo se envia al cerrar el ticket");
    }
    // Step 1: permission for the target status.
    if (reopen) {
      if (actor.role() != Role.USER || !isRequester(ticket, actor)) {
        throw new ForbiddenException("Solo el solicitante puede reabrir su ticket");
      }
      if (!hasText(request.comment()) || request.comment().trim().length() < 5) {
        throw new BadRequestException("Indica el motivo para reabrir el ticket (minimo 5 caracteres)");
      }
    } else if (cancel) {
      if (actor.role() != Role.USER || !isRequester(ticket, actor)) {
        throw new ForbiddenException("Solo el solicitante puede cancelar su ticket");
      }
      if (current != TicketStatus.ABIERTA && current != TicketStatus.ASIGNADA) {
        throw new BusinessRuleException("Solo se puede cancelar un ticket ABIERTA o ASIGNADA");
      }
      String reason = hasText(request.comment()) ? request.comment().trim() : "";
      if (reason.length() < MIN_CANCEL_REASON || reason.length() > MAX_CANCEL_REASON) {
        throw new BadRequestException("Indica el motivo para cancelar el ticket (entre "
            + MIN_CANCEL_REASON + " y " + MAX_CANCEL_REASON + " caracteres)");
      }
    } else if (target == TicketStatus.EN_PROCESO || target == TicketStatus.RESUELTA) {
      if (actor.role() != Role.TECHNICIAN || !isAssignedTechnician(ticket, actor)) {
        throw new ForbiddenException("Solo el tecnico asignado puede cambiar el ticket a " + target);
      }
    } else if (target == TicketStatus.CERRADA) {
      if (actor.role() != Role.USER || !isRequester(ticket, actor)) {
        throw new ForbiddenException("Solo el solicitante puede cerrar su ticket");
      }
    } else {
      throw new BusinessRuleException("Transicion no permitida: el estado " + target
          + " no se establece por este medio (la asignacion se hace con /assign)");
    }
    // Step 2: the step must be the next one in the cycle (or reopen/cancel).
    if (!reopen && !cancel && NEXT_STATUS.get(current) != target) {
      throw new BusinessRuleException("Transicion de estado no permitida: " + current + " -> " + target);
    }
    User changedBy = loadUser(actor.id());
    if (reopen) {
      comments.save(new Comment(ticket, changedBy,
          "Ticket reabierto por el solicitante. Motivo: " + request.comment().trim()));
    }
    // The rating must travel in the same UPDATE that closes the ticket:
    // the trg_tickets_block_closed trigger rejects any later change.
    if (target == TicketStatus.CERRADA) {
      ticket.setRating(request.rating() == null ? null : request.rating().shortValue());
      ticket.setFeedback(hasText(request.feedback()) ? request.feedback().trim() : null);
    }
    // Same for the cancel reason: CANCELADA is final too.
    if (cancel) {
      ticket.setCancelReason(request.comment().trim());
    }
    ticket.setStatus(target);
    Ticket saved = tickets.saveAndFlush(ticket);
    recordHistory(saved, current, target, changedBy);
    return TicketResponse.from(saved);
  }

  // Finds a ticket and confirms the actor may see it.
  // ADMIN sees any; TECHNICIAN the assigned ones; USER the own ones.
  public Ticket findAccessibleTicket(Long ticketId, AuthUser actor) {
    Ticket ticket = tickets.findById(ticketId)
        .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));
    boolean allowed = switch (actor.role()) {
      case ADMIN -> true;
      case TECHNICIAN -> isAssignedTechnician(ticket, actor);
      case USER -> isRequester(ticket, actor);
    };
    if (!allowed) {
      throw new ForbiddenException("No tienes permiso para acceder a este ticket");
    }
    return ticket;
  }

  // Copies title, description, category and priority (trimmed).
  private void applyRequest(Ticket ticket, TicketRequest request) {
    ticket.setTitle(request.title().trim());
    ticket.setDescription(request.description().trim());
    ticket.setCategory(request.category());
    ticket.setPriority(request.priority());
  }

  // Loads a user or answers 404.
  private User loadUser(Long userId) {
    return users.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
  }

  // Saves one history row (null previous = creation).
  private void recordHistory(Ticket ticket, TicketStatus previous, TicketStatus next, User changedBy) {
    history.save(new StatusHistory(ticket, previous, next, changedBy));
  }

  // True when the text has something besides spaces.
  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  // True when the actor reported the ticket.
  private boolean isRequester(Ticket ticket, AuthUser actor) {
    return Objects.equals(ticket.getRequester().getId(), actor.id());
  }

  // True when the actor is the assigned technician.
  private boolean isAssignedTechnician(Ticket ticket, AuthUser actor) {
    return ticket.getTechnician() != null
        && Objects.equals(ticket.getTechnician().getId(), actor.id());
  }
}
