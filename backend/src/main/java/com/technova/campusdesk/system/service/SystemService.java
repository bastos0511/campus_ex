package com.technova.campusdesk.system.service;

import com.technova.campusdesk.comment.entity.Comment;
import com.technova.campusdesk.comment.repository.CommentRepository;
import com.technova.campusdesk.config.StartupValidator;
import com.technova.campusdesk.history.entity.StatusHistory;
import com.technova.campusdesk.history.repository.StatusHistoryRepository;
import com.technova.campusdesk.system.dto.SeedResultResponse;
import com.technova.campusdesk.system.dto.SystemStatusResponse;
import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.ticket.entity.TicketCategory;
import com.technova.campusdesk.ticket.entity.TicketPriority;
import com.technova.campusdesk.ticket.entity.TicketStatus;
import com.technova.campusdesk.ticket.repository.TicketRepository;
import com.technova.campusdesk.user.entity.Role;
import com.technova.campusdesk.user.entity.User;
import com.technova.campusdesk.user.repository.UserRepository;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Environment diagnostics (GET /api/system/status) and the non-destructive
// part of seed.sql (POST /api/system/seed): demo accounts and tickets are only
// inserted when the users table is empty, never deleted or modified.
@Service
public class SystemService {

  private final UserRepository users;
  private final TicketRepository tickets;
  private final CommentRepository comments;
  private final StatusHistoryRepository history;
  private final PasswordEncoder passwords;
  private final DataSource dataSource;
  private final String jwtSecret;

  // Spring only injects constructor dependencies.
  public SystemService(
      UserRepository users,
      TicketRepository tickets,
      CommentRepository comments,
      StatusHistoryRepository history,
      PasswordEncoder passwords,
      DataSource dataSource,
      @Value("${app.jwt.secret:}") String jwtSecret) {
    this.users = users;
    this.tickets = tickets;
    this.comments = comments;
    this.history = history;
    this.passwords = passwords;
    this.dataSource = dataSource;
    this.jwtSecret = jwtSecret;
  }

  // Current prerequisite state. Counts are -1 when they cannot be read.
  @Transactional(readOnly = true)
  public SystemStatusResponse status() {
    boolean dbReachable = false;
    boolean schemaReady = false;
    long userCount = -1;
    long ticketCount = -1;
    try (Connection connection = dataSource.getConnection()) {
      dbReachable = true;
      schemaReady = schemaPresent(connection);
      if (schemaReady) {
        userCount = users.count();
        ticketCount = tickets.count();
      }
    } catch (Exception ex) {
      dbReachable = false;
    }
    return new SystemStatusResponse(
        dbReachable, schemaReady, isJwtConfigured(), userCount, ticketCount);
  }

  // Inserts minimal demo data only on an empty users table. Otherwise it
  // reports the current counts and changes nothing.
  @Transactional
  public SeedResultResponse seedDemoData() {
    long existingUsers = users.count();
    if (existingUsers > 0) {
      return new SeedResultResponse(false,
          "La base ya contiene usuarios; no se modificó nada.",
          existingUsers, tickets.count());
    }
    User admin = users.save(new User("Administrador TechNova",
        "admin@technova.com", passwords.encode("Admin123*"), Role.ADMIN));
    User technician = users.save(new User("Carlos Técnico",
        "tecnico1@technova.com", passwords.encode("Tecnico123*"), Role.TECHNICIAN));
    User requester = users.save(new User("Ana Gómez",
        "usuario1@technova.com", passwords.encode("Usuario123*"), Role.USER));

    Ticket open = newTicket("No enciende el monitor",
        "El monitor del puesto de trabajo no enciende desde esta mañana.",
        TicketCategory.HARDWARE, TicketPriority.MEDIA, TicketStatus.ABIERTA,
        requester, null);
    tickets.save(open);
    history.save(new StatusHistory(open, null, TicketStatus.ABIERTA, requester));

    Ticket assigned = newTicket("Sin acceso a la VPN corporativa",
        "No es posible conectarse a la VPN; aparece un error de autenticación.",
        TicketCategory.REDES, TicketPriority.ALTA, TicketStatus.ASIGNADA,
        requester, technician);
    tickets.save(assigned);
    history.save(new StatusHistory(assigned, null, TicketStatus.ABIERTA, requester));
    history.save(new StatusHistory(assigned, TicketStatus.ABIERTA, TicketStatus.ASIGNADA, admin));

    comments.save(new Comment(assigned, technician,
        "Revisaré tu perfil de VPN y te escribo en breve."));

    return new SeedResultResponse(true,
        "Datos demo creados: 3 usuarios y 2 tickets.",
        users.count(), tickets.count());
  }

  // True when the four schema.sql tables exist.
  private boolean schemaPresent(Connection connection) throws Exception {
    DatabaseMetaData meta = connection.getMetaData();
    List<String> tables = new ArrayList<>();
    try (ResultSet rs = meta.getTables(
        connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
      while (rs.next()) {
        tables.add(rs.getString("TABLE_NAME").toLowerCase());
      }
    }
    return missingTables(new TreeSet<>(tables)).isEmpty();
  }

  // Delegates to the validator rules without duplicating them.
  private List<String> missingTables(TreeSet<String> found) {
    return StartupValidator.missingTables(found, StartupValidator.REQUIRED_TABLES);
  }

  private boolean isJwtConfigured() {
    return StartupValidator.isSecretValid(jwtSecret);
  }

  private Ticket newTicket(String title, String description, TicketCategory category,
      TicketPriority priority, TicketStatus status, User requester, User technician) {
    Ticket ticket = new Ticket();
    ticket.setTitle(title);
    ticket.setDescription(description);
    ticket.setCategory(category);
    ticket.setPriority(priority);
    ticket.setStatus(status);
    ticket.setRequester(requester);
    ticket.setTechnician(technician);
    return ticket;
  }
}
