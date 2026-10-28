package com.technova.campusdesk.attachment.dto;

import com.technova.campusdesk.attachment.entity.TicketAttachment;
import com.technova.campusdesk.attachment.entity.TipoContenido;
import com.technova.campusdesk.user.dto.UserSummary;
import java.time.Instant;

public record AttachmentResponse(
    Long id,
    String nombreArchivo,
    String urlOReferencia,
    TipoContenido tipoContenido,
    Instant fechaCreacion,
    Long ticketId,
    UserSummary subidoPor) {

  public static AttachmentResponse from(TicketAttachment attachment) {
    return new AttachmentResponse(attachment.getId(), attachment.getNombreArchivo(),
        attachment.getUrlOReferencia(), attachment.getTipoContenido(),
        attachment.getFechaCreacion(), attachment.getTicket().getId(),
        UserSummary.from(attachment.getSubidoPor()));
  }
}
