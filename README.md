# CampusDesk — TechNova Solutions Help Desk

Web platform for managing technology incidents. Team members report
Hardware, Software, Network, Access or Other issues, and the tech team
handles them through the cycle
`ABIERTA → ASIGNADA → EN_PROCESO → RESUELTA → CERRADA`, with comments,
change history, roles and JWT authentication.

## Members

- Julian Ovalle
- Camilo Vega
- Juan Bastos
- Crisian Rojas

## Technologies

- **Frontend:** HTML5, CSS3, Vanilla JavaScript (`fetch()` + `async/await`).
- **Backend:** Java 21, Spring Boot 3.3, Spring Data JPA, Spring Security + JWT,
  Bean Validation, Swagger (springdoc).
- **Database:** PostgreSQL 14+.

## Requirements

- JDK 21+
- PostgreSQL 14+ (local o Docker)
- Maven **no** necesita instalación global: el proyecto incluye Maven Wrapper (`backend/mvnw.cmd`)
- Un servidor estático para el frontend (p. ej. `python -m http.server 5500` o Live Server de VS Code)

## Despliegue con Docker (recomendado)

Solo necesitas Docker. Desde la raíz del repositorio clonado:

```bash
docker compose up -d --build
```

Levanta PostgreSQL (con `schema.sql` y `seed.sql` la primera vez), el backend
y el frontend:

- Frontend: http://localhost:5500
- API: http://localhost:8080/api · Swagger: http://localhost:8080/swagger-ui.html
- Entra con `admin@campusdesk.com` / `Admin96c2b7d5!` (ver "Configuración").

Para borrar también los datos: `docker compose down -v`.

## Installation and PostgreSQL

`database/schema.sql` es la única fuente de verdad: crea tablas,
índices, CHECKs, triggers y la vista de estadísticas. Hibernate solo
**valida** la estructura al arrancar (`ddl-auto=validate`) y nunca
la modifica. La zona horaria de la sesión es UTC para que el trigger
`updated_at` coincida con las fechas JPA (ver `application.properties`).

La base de datos es infraestructura externa y se crea **una sola vez** con `psql`:

```bash
CREATE USER campusdesk_app WITH PASSWORD 'your-password';
CREATE DATABASE technova_db OWNER campusdesk_app;
psql -U campusdesk_app -d technova_db -f database/schema.sql
psql -U campusdesk_app -d technova_db -f database/seed.sql
```

`seed.sql` carga 5 cuentas, 5 tickets (uno por estado), historial y comentarios.
Alternativa sin `psql`: arranca el backend con el esquema ya aplicado y vacío,
inicia sesión como ADMIN y llama a `POST /api/system/seed` (solo inserta
datos demo si la tabla de usuarios está vacía; nunca borra ni modifica nada).

> WARNING: `schema.sql` empieza con `DROP TABLE ... CASCADE` y vacía sus
> tablas. Ejecuta esos comandos SOLO sobre una base vacía. No existe
> ningún endpoint ni proceso que borre la base de datos: el reseteo
> destructivo quedó eliminado a propósito.

## Configuración

Proyecto de desarrollo (no va a producción): toda la configuración está fija
en `backend/src/main/resources/application.properties`, sin variables de
entorno obligatorias.

| Valor | Configurado |
|---|---|
| Base de datos | `technova_db`, usuario `campusdesk_app`, clave `campusdesk_dev` (igual que `docker-compose.yml`) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/technova_db` (Docker lo cambia al host `postgres`) |
| JWT | Secreto fijo de 64 caracteres, tokens de 60 minutos |
| ADMIN creado al arrancar | `admin@campusdesk.com` / `Admin96c2b7d5!` |
| TECHNICIAN creado al arrancar | `tecnico@campusdesk.com` / `Tecnico3f8a1c6e!` |
| Cierre automático | Tickets `RESUELTA` tras 7 días, a las 3 a. m. |

## Running

Backend (con PostgreSQL ya levantado, p. ej. `docker compose up -d postgres`):

Desde IntelliJ IDEA: abre la carpeta `backend/` como proyecto Maven y ejecuta
la clase `com.technova.campusdesk.CampusDeskApplication` (contiene
`@SpringBootApplication`).

O con Maven Wrapper (sin instalación global de Maven):

```bash
cd backend
mvnw.cmd spring-boot:run
```

Al arrancar, el backend valida su entorno y falla rápido con un mensaje
claro si PostgreSQL no es alcanzable o si el
esquema está incompleto. Diagnóstico sin login:

```bash
curl http://localhost:8080/api/system/status
```

Frontend (desde la carpeta `frontend/`, puerto 5500 exigido por CORS):

```bash
cd frontend
python -m http.server 5500
```

Abre `http://localhost:5500/pages/login.html`.

## Roles

| Role | Can |
|---|---|
| `USER` | Register, create and view own tickets, comment, close (and rate) or reopen own `RESUELTA` |
| `TECHNICIAN` | View assigned, move to `EN_PROCESO`/`RESUELTA`, comment, write internal notes |
| `ADMIN` | View everything, list technicians/users, assign and reassign, write internal notes |

Public registration always creates `USER`. `ADMIN`/`TECHNICIAN` only come
from startup provisioning. Demo accounts (seed data, development only):
`admin@technova.com` (ADMIN), `tecnico1@technova.com` (TECHNICIAN),
`usuario1@technova.com` (USER). Passwords are never published: configure
yours locally (see "Local credentials" below).

## Funcionalidades añadidas

| Funcionalidad | Backend | Frontend |
|---|---|---|
| **Búsqueda y paginación** | `GET /api/tickets/search?q=&status=&priority=&category=&overdue=&page=&size=` responde `{ items, page, size, totalItems, totalPages }`. `q` busca en título, descripción, solicitante y `#id`. `GET /api/tickets` sigue igual (lo usan dashboard y administración). | `tickets.html`: búsqueda en el servidor, casilla "Solo vencidos", botones Anterior/Siguiente (10 por página). |
| **Reabrir ticket** | El solicitante pasa `RESUELTA → EN_PROCESO` con `PATCH /status` `{ "status": "EN_PROCESO", "comment": "motivo" }`. El motivo (mín. 5 caracteres) queda como comentario y el cambio en el historial. | Botón "No se resolvió: reabrir" con formulario de motivo. El historial indica cuántas veces se reabrió. |
| **Calificación al cerrar** | `PATCH /status` `{ "status": "CERRADA", "rating": 1-5, "feedback": "..." }` (ambos opcionales). Se guarda en el mismo UPDATE que cierra, porque el trigger bloquea cambios en tickets `CERRADA`. | Botón "Cerrar y calificar" con estrellas y comentario. El detalle muestra la calificación. |
| **Notas internas** | `POST /comments` `{ "content": "...", "internal": true }`, solo `TECHNICIAN`/`ADMIN`. Un `USER` nunca las recibe. | Casilla "Nota interna" (solo personal). Las notas se ven con borde punteado y etiqueta. |
| **SLA (fecha límite)** | `due_at = created_at + horas de la prioridad` (CRITICA 4 h, ALTA 24 h, MEDIA 72 h, BAJA 120 h). Se recalcula si se edita la prioridad. La respuesta incluye `dueAt` y `overdue`. | Etiqueta "Vencido" en las tarjetas (listado y dashboard) y fecha límite en el detalle. |
| **Cancelar ticket** | El solicitante pasa `ABIERTA`/`ASIGNADA → CANCELADA` con `PATCH /status` `{ "status": "CANCELADA", "comment": "motivo" }` (5-500 caracteres). El motivo se guarda en `cancel_reason` (`cancelReason` en la respuesta). `CANCELADA` es estado final: el trigger bloquea cambios y comentarios. Base existente: `database/migrations/002_cancelar_ticket.sql`. | — (solo backend) |
| **Cierre automático** | Tarea programada (`AutoCloseJob`) que cierra los tickets en `RESUELTA` tras `AUTO_CLOSE_DAYS` días, con un comentario que lo explica. | — |

**Si tu base de datos ya existía**, aplica la migración (no borra datos):

```bash
psql -U campusdesk_app -d technova_db -f database/migrations/001_sla_calificacion_notas_internas.sql
```

Si creas la base desde cero, `schema.sql` y `seed.sql` ya incluyen todo.

## Swagger

With the backend running: `http://localhost:8080/swagger-ui.html`.
Log in via `POST /api/auth/login`, press **Authorize** and paste the token.

## System endpoints (reemplazo de los antiguos scripts)

| Endpoint | Auth | Hace |
|---|---|---|
| `GET /api/system/status` | Pública | Diagnóstico: BD alcanzable, esquema listo, JWT configurado, conteos |
| `POST /api/system/seed` | ADMIN | Inserta datos demo solo si la BD está vacía (201) o informa sin modificar nada (200) |
