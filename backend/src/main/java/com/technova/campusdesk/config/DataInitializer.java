package com.technova.campusdesk.config;

import com.technova.campusdesk.user.entity.Role;
import com.technova.campusdesk.user.entity.User;
import com.technova.campusdesk.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

// Creates the ADMIN and TECHNICIAN accounts on startup (registration only makes USER).
// Only acts when their email and password are defined in the environment.
@Configuration
public class DataInitializer {

  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

  private final UserRepository users;
  private final PasswordEncoder passwords;

  // Seed names, emails and passwords (empty = skip that role).
  @Value("${app.seed.admin-name:}")
  private String adminName;
  @Value("${app.seed.admin-email:}")
  private String adminEmail;
  @Value("${app.seed.admin-password:}")
  private String adminPassword;
  @Value("${app.seed.technician-name:}")
  private String technicianName;
  @Value("${app.seed.technician-email:}")
  private String technicianEmail;
  @Value("${app.seed.technician-password:}")
  private String technicianPassword;

  public DataInitializer(UserRepository users, PasswordEncoder passwords) {
    this.users = users;
    this.passwords = passwords;
  }

  // Runs once when the application starts.
  @Bean
  public ApplicationRunner seedAccounts() {
    return args -> {
      seed(adminName, adminEmail, adminPassword, Role.ADMIN);
      seed(technicianName, technicianEmail, technicianPassword, Role.TECHNICIAN);
    };
  }

  // Creates the account when email and password exist and it is not a duplicate.
  private void seed(String name, String email, String password, Role role) {
    if (email == null || email.isBlank() || password == null || password.isBlank()) {
      log.info("No se aprovisiona el rol {} (faltan variables de entorno)", role);
      return;
    }
    String cleanEmail = email.trim().toLowerCase();
    if (users.existsByEmail(cleanEmail)) {
      return;
    }
    String cleanName = (name == null || name.isBlank()) ? role.name() : name.trim();
    users.save(new User(cleanName, cleanEmail, passwords.encode(password), role));
    // The password is never logged, here or anywhere.
    log.info("Cuenta {} aprovisionada: {}", role, cleanEmail);
  }
}
