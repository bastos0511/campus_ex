package com.technova.campusdesk.security;

import com.technova.campusdesk.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

// Answers 401 when authentication is missing or the token is useless.
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper json;

  public RestAuthenticationEntryPoint(ObjectMapper json) {
    this.json = json;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException ex) throws IOException {
    ApiError body = ApiError.of(HttpStatus.UNAUTHORIZED,
        "Autenticacion requerida o token invalido o expirado",
        request.getRequestURI(), List.of());
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
    json.writeValue(response.getWriter(), body);
  }
}
