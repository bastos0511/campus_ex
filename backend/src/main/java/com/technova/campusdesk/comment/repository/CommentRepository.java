package com.technova.campusdesk.comment.repository;

import com.technova.campusdesk.comment.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Access to the comments table. Spring writes the code.
public interface CommentRepository extends JpaRepository<Comment, Long> {

  // Comments of a ticket, oldest first.
  List<Comment> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);

  // Only public comments (what the requester is allowed to see).
  List<Comment> findByTicketIdAndInternalFalseOrderByCreatedAtAscIdAsc(Long ticketId);
}
