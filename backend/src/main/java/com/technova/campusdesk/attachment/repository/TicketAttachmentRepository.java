package com.technova.campusdesk.attachment.repository;

import com.technova.campusdesk.attachment.entity.TicketAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

  List<TicketAttachment> findByTicketIdOrderByFechaCreacionAscIdAsc(Long ticketId);

  Optional<TicketAttachment> findByIdAndTicketId(Long id, Long ticketId);
}
