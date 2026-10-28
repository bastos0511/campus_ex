package com.technova.campusdesk.security;

import com.technova.campusdesk.user.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Security rules: what is public, JWT without sessions, and limited CORS.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables the controllers' @PreAuthorize
public class SecurityConfig {

  private final JwtService jwt;
  private final UserRepository users;
  private final RestAuthenticationEntryPoint entryPoint;
  private final RestAccessDeniedHandler deniedHandler;
  private final String allowedOrigins;

  public SecurityConfig(JwtService jwt, UserRepository users,
      RestAuthenticationEntryPoint entryPoint, RestAccessDeniedHandler deniedHandler,
      @Value("${app.cors.allowed-origins}") String allowedOrigins) {
    this.jwt = jwt;
    this.users = users;
    this.entryPoint = entryPoint;
    this.deniedHandler = deniedHandler;
    this.allowedOrigins = allowedOrigins;
  }

  // Password encryptor (BCrypt).
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // Spring Security filter chain.
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // sessionless API: no CSRF
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(errors -> errors
            .authenticationEntryPoint(entryPoint) // JSON 401
            .accessDeniedHandler(deniedHandler)) // JSON 403
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight
            .requestMatchers("/api/auth/**").permitAll() // login and register
            .requestMatchers(HttpMethod.GET, "/api/system/status").permitAll() // setup diagnostics
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .anyRequest().authenticated()) // everything else needs a token
        .addFilterBefore(new JwtAuthenticationFilter(jwt, users),
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  // Only accepts requests from the given frontend (no wildcards).
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    List<String> origins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    config.setAllowCredentials(false);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
