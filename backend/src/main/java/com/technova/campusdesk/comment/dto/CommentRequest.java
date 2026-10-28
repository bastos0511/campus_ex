package com.technova.campusdesk.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Text of a new comment. internal=true makes it a note only staff can see.
public record CommentRequest(
    @NotBlank(message = "El comentario no puede estar vacio")
    @Size(max = 2000, message = "El comentario es muy largo")
    String content,

    Boolean internal) {
}
