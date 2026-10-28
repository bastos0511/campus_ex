package com.technova.campusdesk.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Fail-fast startup checks. The backend verifies its own environment on
// boot (JWT secret, database connectivity, schema tables) and explains
// exactly what is missing instead of failing with a cryptic error.
@Component
@Order(1)
public class StartupValidator implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

  // Tables created by database/schema.sql (the single source of truth).
  // Hibernate only validates them (ddl-auto=validate), never creates them.
  public static final List<String> REQUIRED_TABLES =
      List.of("users", "tickets", "comments", "ticket_history");

  private final DataSource dataSource;
  private final String jwtSecret;

  public StartupValidator(
      DataSource dataSource,
      @Value("${app.jwt.secret:}") String jwtSecret) {
    this.dataSource = dataSource;
    this.jwtSecret = jwtSecret;
  }

  // Runs before anything else (DataInitializer has no explicit order).
  @Override
  public void run(ApplicationArguments args) {
    if (!isSecretValid(jwtSecret)) {
      throw new IllegalStateException(
          "JWT_SECRET debe tener al menos 32 caracteres. "
              + "Configura la variable de entorno JWT_SECRET antes de arrancar "
              + "(ver application-local.properties.example). Arranque abortado.");
    }
    List<String> tables;
    try (Connection connection = dataSource.getConnection()) {
      tables = readTables(connection);
    } catch (Exception ex) {
      throw new IllegalStateException(
          "No se pudo conectar a PostgreSQL. Revisa DB_URL, DB_USER y DB_PASSWORD "
              + "y que PostgreSQL esté en marcha. "
              + "La base y el esquema se crean una sola vez con psql "
              + "(database/schema.sql). Arranque abortado.", ex);
    }
    List<String> missing = missingTables(new TreeSet<>(tables), REQUIRED_TABLES);
    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "Esquema incompleto: faltan las tablas " + missing + ". "
              + "Aplica database/schema.sql sobre una base vacía con psql "
              + "(una sola vez, es infraestructura externa). Arranque abortado.");
    }
    long users = countUsers();
    log.info("CampusDesk listo: esquema verificado ({} tablas), usuarios registrados: {}",
        REQUIRED_TABLES.size(), users);
  }

  // Reads table names from the current connection.
  private List<String> readTables(Connection connection) throws Exception {
    DatabaseMetaData meta = connection.getMetaData();
    List<String> tables = new ArrayList<>();
    try (ResultSet rs = meta.getTables(
        connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
      while (rs.next()) {
        tables.add(rs.getString("TABLE_NAME").toLowerCase());
      }
    }
    return tables;
  }

  // Counts users without going through JPA (repositories may not be ready yet).
  private long countUsers() {
    try (Connection connection = dataSource.getConnection();
        var statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM users")) {
      return rs.next() ? rs.getLong(1) : -1;
    } catch (Exception ex) {
      log.warn("No se pudo contar los usuarios al arrancar: {}", ex.getMessage());
      return -1;
    }
  }

  // A secret is valid when it reaches 32 UTF-8 bytes (same rule as JwtService).
  // Public so SystemService and unit tests reuse the exact same rule.
  public static boolean isSecretValid(String secret) {
    if (secret == null) {
      return false;
    }
    try {
      return secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length >= 32;
    } catch (Exception ex) {
      return false;
    }
  }

  // Required tables not present in the database (empty = schema ready).
  // Public so SystemService and unit tests reuse the exact same rule.
  public static List<String> missingTables(Set<String> found, List<String> required) {
    List<String> missing = new ArrayList<>();
    for (String table : required) {
      if (!found.contains(table.toLowerCase())) {
        missing.add(table);
      }
    }
    return missing;
  }
}
