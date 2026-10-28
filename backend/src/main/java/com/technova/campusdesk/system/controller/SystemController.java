package com.technova.campusdesk.system.controller;

import com.technova.campusdesk.system.dto.SeedResultResponse;
import com.technova.campusdesk.system.dto.SystemStatusResponse;
import com.technova.campusdesk.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Runtime environment diagnostics and non-destructive demo seeding,
// managed entirely by Spring Boot.
@RestController
@RequestMapping("/api/system")
public class SystemController {

  private final SystemService systemService;

  // Spring only injects constructor dependencies.
  public SystemController(SystemService systemService) {
    this.systemService = systemService;
  }

  // Prerequisite snapshot: database, schema, JWT and row counts.
  // Public so a fresh setup can be diagnosed before logging in.
  @GetMapping("/status")
  @SecurityRequirements // removes the Bearer lock in Swagger
  @Operation(summary = "Environment status (database, schema, JWT, counts)")
  public SystemStatusResponse status() {
    return systemService.status();
  }

  // Inserts demo accounts and tickets only when the users table is empty.
  // ADMIN only. Never deletes or modifies existing data. Answers 201 when
  // it seeds, 200 when there was already data.
  @PostMapping("/seed")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Insert demo data (only on an empty database)")
  public ResponseEntity<SeedResultResponse> seed() {
    SeedResultResponse result = systemService.seedDemoData();
    HttpStatus status = result.seeded() ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(result);
  }
}
