package com.technova.campusdesk.history.repository;

import com.technova.campusdesk.history.entity.StatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Access to the ticket_history table. Spring writes the code.
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

  // History of a ticket, oldest change first.
  List<StatusHistory> findByTicketIdOrderByChangedAtAscIdAsc(Long ticketId);
}
