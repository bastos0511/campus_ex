package com.technova.campusdesk.attachment.dto;

import com.technova.campusdesk.attachment.entity.TipoContenido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttachmentRequest(
    @NotBlank(message = "El nombre del archivo es obligatorio")
    @Size(max = 255, message = "El nombre del archivo es muy largo")
    String nombreArchivo,

    @NotBlank(message = "La URL o referencia es obligatoria")
    @Size(max = 2000, message = "La URL o referencia es muy larga")
    String urlOReferencia,

    @NotNull(message = "El tipo de contenido es obligatorio")
    TipoContenido tipoContenido) {
}
