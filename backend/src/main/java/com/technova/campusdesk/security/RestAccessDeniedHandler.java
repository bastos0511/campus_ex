package com.technova.campusdesk.security;

import com.technova.campusdesk.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

// Answers 403 when the user is in but has no permission.
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper json;

  public RestAccessDeniedHandler(ObjectMapper json) {
    this.json = json;
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException ex) throws IOException {
    ApiError body = ApiError.of(HttpStatus.FORBIDDEN,
        "No tienes permiso para realizar esta operacion",
        request.getRequestURI(), List.of());
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
    json.writeValue(response.getWriter(), body);
  }
}
