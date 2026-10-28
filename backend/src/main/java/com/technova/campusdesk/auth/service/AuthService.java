package com.technova.campusdesk.auth.service;

import com.technova.campusdesk.auth.dto.AuthResponse;
import com.technova.campusdesk.auth.dto.LoginRequest;
import com.technova.campusdesk.auth.dto.RegisterRequest;
import com.technova.campusdesk.user.dto.UserResponse;
import com.technova.campusdesk.user.entity.Role;
import com.technova.campusdesk.user.entity.User;
import com.technova.campusdesk.exception.DuplicateResourceException;
import com.technova.campusdesk.user.repository.UserRepository;
import com.technova.campusdesk.security.AuthUser;
import com.technova.campusdesk.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// User login and registration.
@Service
public class AuthService {

  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final JwtService jwt;

  // Spring only injects constructor dependencies.
  public AuthService(UserRepository users, PasswordEncoder passwords, JwtService jwt) {
    this.users = users;
    this.passwords = passwords;
    this.jwt = jwt;
  }

  // Creates a new account. Always born USER, ignores whatever the client sends.
  @Transactional
  public UserResponse register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase();
    if (users.existsByEmail(email)) {
      throw new DuplicateResourceException("El correo ya esta registrado");
    }
    User user = new User(request.fullName().trim(), email,
        passwords.encode(request.password()), Role.USER);
    return UserResponse.from(users.save(user));
  }

  // Returns the current authenticated user (fresh from the database).
  @Transactional(readOnly = true)
  public UserResponse me(AuthUser actor) {
    return users.findById(actor.id())
        .map(UserResponse::from)
        .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));
  }

  // Checks email and password, then returns the JWT token.
  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    String email = request.email().trim().toLowerCase();
    User user = users.findByEmail(email)
        .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));
    // Same message when the password fails: never reveals which field was wrong.
    if (!passwords.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException("Credenciales invalidas");
    }
    return new AuthResponse(jwt.generateToken(user), "Bearer",
        jwt.getExpirationSeconds(), UserResponse.from(user));
  }
}
