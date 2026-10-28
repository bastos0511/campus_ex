# Entrega del examen — Adjuntos de tickets (TicketAttachment)

Funcionalidad para asociar evidencias de soporte (capturas, logs en texto o enlaces
externos) a un ticket. Solo se guardan los metadatos y la referencia, no el archivo.

Ruta base del código: `backend/src/main/java/com/technova/campusdesk/`

## Archivos creados

| Archivo | Descripción |
|---|---|
| `attachment/entity/TicketAttachment.java` | Entidad JPA: `id`, `nombreArchivo`, `urlOReferencia`, `tipoContenido`, `fechaCreacion`, relación `@ManyToOne` con `Ticket` y con `User` (`subidoPor`) |
| `attachment/entity/TipoContenido.java` | Enum: `IMAGEN`, `LOG_TEXTO`, `ENLACE_EXTERNO` |
| `attachment/dto/AttachmentRequest.java` | DTO de entrada (`nombreArchivo`, `urlOReferencia`, `tipoContenido`) con `@NotBlank`, `@Size` y `@NotNull` |
| `attachment/dto/AttachmentResponse.java` | DTO de salida con los datos del adjunto, `ticketId` y el usuario que lo subió |
| `attachment/repository/TicketAttachmentRepository.java` | Repositorio Spring Data: listado por ticket y búsqueda por `id` + `ticketId` |
| `attachment/service/AttachmentService.java` | Reglas de negocio: permisos, validación de estado del ticket y de formato de URL |
| `database/migrations/003_adjuntos_ticket.sql` | Migración para crear la tabla `ticket_attachments` en una base existente |

## Archivos modificados

| Archivo | Cambio |
|---|---|
| `ticket/controller/TicketController.java` | Se inyecta `AttachmentService` y se agregan los 3 endpoints de adjuntos |
| `database/schema.sql` | Se agrega la tabla `ticket_attachments` (FK, CHECK de `tipo_contenido`, índices) y su `DROP TABLE` |

## Endpoints implementados

Todos requieren autenticación (JWT en `Authorization: Bearer <token>`).

### `POST /api/tickets/{id}/adjuntos`

Registra los metadatos de una evidencia.

```json
{
  "nombreArchivo": "error-login.png",
  "urlOReferencia": "https://repo.interno.local/capturas/error-login.png",
  "tipoContenido": "ENLACE_EXTERNO"
}
```

| Caso | Respuesta |
|---|---|
| Propietario del ticket o técnico asignado, ticket en `ABIERTA`, `ASIGNADA` o `EN_PROCESO` | `201 Created` con el adjunto |
| Ticket en `RESUELTA`, `CERRADA` (o `CANCELADA`) | `400` — "No se permiten adjuntos en tickets finalizados" |
| `tipoContenido = ENLACE_EXTERNO` y la URL no es `http`/`https` válida con host | `400` |
| Campos vacíos, demasiado largos o tipo inválido | `400` |
| Usuario que no es propietario ni técnico asignado (incluido ADMIN) | `403` |
| Ticket inexistente | `404` |

### `DELETE /api/tickets/{id}/adjuntos/{adjuntoId}`

| Caso | Respuesta |
|---|---|
| Quien registró el adjunto o un `ADMIN` | `204 No Content` |
| Cualquier otro usuario | `403` |
| Ticket o adjunto inexistente, o el adjunto no pertenece a ese ticket | `404` |

### `GET /api/tickets/{id}/adjuntos`

Lista los adjuntos del ticket, del más antiguo al más reciente.

| Caso | Respuesta |
|---|---|
| `ADMIN`, técnico asignado o propietario del ticket | `200 OK` con la lista |
| Otro usuario | `403` |
| Ticket inexistente | `404` |

## Validación de URL

Se hace manualmente en `AttachmentService.isValidUrl` solo cuando el tipo es
`ENLACE_EXTERNO`: la referencia debe ser una URI absoluta con esquema `http` o
`https` y con host. Para `IMAGEN` y `LOG_TEXTO` se acepta cualquier referencia no vacía.

## Cómo verificar

1. Base de datos:
   - Desde cero: `schema.sql` ya incluye la tabla.
   - Base existente: `psql -U campusdesk_app -d technova_db -f database/migrations/003_adjuntos_ticket.sql`
2. Levantar el backend (`ddl-auto=validate` confirma que la entidad coincide con la tabla).
3. Probar los endpoints en Swagger: `http://localhost:8080/swagger-ui.html`.

## Commits

```
feat(db): add ticket_attachments table and migration 003
feat(attachment): add attachment service and /api/tickets/{id}/adjuntos endpoints
agregar attachments y dto
```
