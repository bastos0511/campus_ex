package com.technova.campusdesk.security;

import com.technova.campusdesk.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Creates and verifies signed JWT tokens.
@Service
public class JwtService {

  private final SecretKey key;
  private final long expirationMillis;

  // The secret comes from the environment and must be 32+ characters.
  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
    }
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMillis = expirationMinutes * 60_000;
  }

  // Generates a token with the email as owner and the role as extra data.
  public String generateToken(User user) {
    Date now = new Date();
    return Jwts.builder()
        .subject(user.getEmail())
        .claim("role", user.getRole().name())
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expirationMillis))
        .signWith(key)
        .compact();
  }

  // Lifetime in seconds (reported at login).
  public long getExpirationSeconds() {
    return expirationMillis / 1000;
  }

  // Reads a token and confirms signature and validity. Throws JwtException otherwise.
  public Claims parse(String token) throws JwtException {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
