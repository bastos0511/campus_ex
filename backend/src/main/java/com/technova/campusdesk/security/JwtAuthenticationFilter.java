package com.technova.campusdesk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// Filter running on each request: reads the token and marks who is coming in.
// Never answers 401 here; the entry point decides that later.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwt;
  private final com.technova.campusdesk.user.repository.UserRepository users;

  public JwtAuthenticationFilter(JwtService jwt,
      com.technova.campusdesk.user.repository.UserRepository users) {
    this.jwt = jwt;
    this.users = users;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    // Without "Bearer ...", it continues unauthenticated.
    if (header != null && header.startsWith("Bearer ")) {
      try {
        String token = header.substring(7).trim();
        Claims claims = jwt.parse(token);
        String email = claims.getSubject();
        // The user is loaded from the DB: the real role wins, not the token's.
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          users.findByEmail(email).ifPresent(user -> {
            AuthUser principal = new AuthUser(user.getId(), user.getEmail(),
                user.getFullName(), user.getRole());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(auth);
          });
        }
      } catch (JwtException | IllegalArgumentException ex) {
        // Bad or expired token: clear and continue unauthenticated.
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
