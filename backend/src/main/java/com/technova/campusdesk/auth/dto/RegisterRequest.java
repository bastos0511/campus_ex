package com.technova.campusdesk.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Data for creating an account. No role field: always born USER.
public record RegisterRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre es muy largo")
    String fullName,

    @NotBlank(message = "El correo es obligatorio")
    @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "El correo no es valido")
    @Size(max = 150, message = "El correo es muy largo")
    String email,

    // 8-64 chars with uppercase, lowercase and digit.
    @NotBlank(message = "La clave es obligatoria")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$",
        message = "La clave necesita mayuscula, minuscula y numero")
    String password) {
}
