package com.technova.campusdesk.auth.dto;

import jakarta.validation.constraints.NotBlank;

// Login input data.
public record LoginRequest(
    @NotBlank(message = "El correo es obligatorio")
    String email,

    @NotBlank(message = "La clave es obligatoria")
    String password) {
}
