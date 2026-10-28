package com.technova.campusdesk.attachment.service;

import com.technova.campusdesk.attachment.dto.AttachmentRequest;
import com.technova.campusdesk.attachment.dto.AttachmentResponse;
import com.technova.campusdesk.attachment.entity.TicketAttachment;
import com.technova.campusdesk.attachment.entity.TipoContenido;
import com.technova.campusdesk.attachment.repository.TicketAttachmentRepository;
import com.technova.campusdesk.exception.BadRequestException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.security.AuthUser;
import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import com.technova.campusdesk.ticket.repository.TicketRepository;
import com.technova.campusdesk.ticket.service.TicketService;
import com.technova.campusdesk.user.entity.Role;
import com.technova.campusdesk.user.entity.User;
import com.technova.campusdesk.user.repository.UserRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttachmentService {

  private static final Set<TicketStatus> OPEN_STATUSES =
      EnumSet.of(TicketStatus.ABIERTA, TicketStatus.ASIGNADA, TicketStatus.EN_PROCESO);

  private final TicketAttachmentRepository attachments;
  private final TicketRepository tickets;
  private final UserRepository users;
  private final TicketService ticketService;

  public AttachmentService(TicketAttachmentRepository attachments, TicketRepository tickets,
      UserRepository users, TicketService ticketService) {
    this.attachments = attachments;
    this.tickets = tickets;
    this.users = users;
    this.ticketService = ticketService;
  }

  @Transactional(readOnly = true)
  public List<AttachmentResponse> list(Long ticketId, AuthUser actor) {
    ticketService.findAccessibleTicket(ticketId, actor);
    return attachments.findByTicketIdOrderByFechaCreacionAscIdAsc(ticketId)
        .stream().map(AttachmentResponse::from).toList();
  }

  @Transactional
  public AttachmentResponse add(Long ticketId, AttachmentRequest request, AuthUser actor) {
    Ticket ticket = tickets.findById(ticketId)
        .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));
    if (!isRequester(ticket, actor) && !isAssignedTechnician(ticket, actor)) {
      throw new ForbiddenException(
          "Solo el propietario del ticket o el tecnico asignado pueden registrar adjuntos");
    }
    if (!OPEN_STATUSES.contains(ticket.getStatus())) {
      throw new BadRequestException("No se permiten adjuntos en tickets finalizados");
    }
    String reference = request.urlOReferencia().trim();
    if (request.tipoContenido() == TipoContenido.ENLACE_EXTERNO && !isValidUrl(reference)) {
      throw new BadRequestException(
          "La URL no tiene un formato valido (debe empezar por http:// o https://)");
    }
    User uploader = users.findById(actor.id())
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    TicketAttachment saved = attachments.save(new TicketAttachment(ticket, uploader,
        request.nombreArchivo().trim(), reference, request.tipoContenido()));
    return AttachmentResponse.from(saved);
  }

  @Transactional
  public void delete(Long ticketId, Long attachmentId, AuthUser actor) {
    if (!tickets.existsById(ticketId)) {
      throw new ResourceNotFoundException("Ticket no encontrado");
    }
    TicketAttachment attachment = attachments.findByIdAndTicketId(attachmentId, ticketId)
        .orElseThrow(() -> new ResourceNotFoundException("Adjunto no encontrado"));
    boolean isUploader = Objects.equals(attachment.getSubidoPor().getId(), actor.id());
    if (!isUploader && actor.role() != Role.ADMIN) {
      throw new ForbiddenException(
          "Solo quien registro el adjunto o un administrador pueden eliminarlo");
    }
    attachments.delete(attachment);
  }

  private static boolean isValidUrl(String value) {
    try {
      URI uri = new URI(value);
      String scheme = uri.getScheme();
      return scheme != null
          && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
          && uri.getHost() != null;
    } catch (URISyntaxException ex) {
      return false;
    }
  }

  private static boolean isRequester(Ticket ticket, AuthUser actor) {
    return Objects.equals(ticket.getRequester().getId(), actor.id());
  }

  private static boolean isAssignedTechnician(Ticket ticket, AuthUser actor) {
    return ticket.getTechnician() != null
        && Objects.equals(ticket.getTechnician().getId(), actor.id());
  }
}
