package com.technova.campusdesk.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Swagger title + "Authorize" button with JWT token.
@Configuration
@OpenAPIDefinition(
    info = @Info(title = "CampusDesk API", version = "1.0.0",
        description = "Technology incident management"),
    security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Paste the token from POST /api/auth/login (without the Bearer word)")
public class OpenApiConfig {
}
