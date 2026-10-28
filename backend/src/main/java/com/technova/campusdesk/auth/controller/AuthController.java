package com.technova.campusdesk.auth.controller;

import com.technova.campusdesk.auth.dto.AuthResponse;
import com.technova.campusdesk.auth.dto.LoginRequest;
import com.technova.campusdesk.auth.dto.RegisterRequest;
import com.technova.campusdesk.user.dto.UserResponse;
import com.technova.campusdesk.auth.service.AuthService;
import com.technova.campusdesk.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Login and registration. The only public routes (no token).
@RestController
@RequestMapping("/api/auth")
@SecurityRequirements // removes the Bearer lock in Swagger
public class AuthController {

  private final AuthService authService;

  // Spring only injects constructor dependencies.
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  // Creates an account (always USER). Answers 201.
  @PostMapping("/register")
  @Operation(summary = "Register user (always creates USER role)")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  // Logs in and returns the JWT token.
  @PostMapping("/login")
  @Operation(summary = "Log in, returns JWT")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  // Current authenticated user (validates the token against the database).
  @GetMapping("/me")
  @Operation(summary = "Current user from a valid token")
  public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
    return authService.me(actor);
  }
}
