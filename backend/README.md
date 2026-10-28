# CampusDesk · Backend (Java + Spring Boot)

Help-desk REST API. The frontend (`../`) consumes it with `fetch()`.

## Requirements

- JDK 21+ and Maven 3.9+
- PostgreSQL 14+ with schema and data from `../database/` (`schema.sql` + `seed.sql`)

```sql
CREATE USER campusdesk_app WITH PASSWORD 'your-password';
CREATE DATABASE technova_db OWNER campusdesk_app;
```

```bash
psql -U campusdesk_app -d technova_db -f database/schema.sql
psql -U campusdesk_app -d technova_db -f database/seed.sql
```

> WARNING: `schema.sql` drops its tables first (`DROP TABLE ... CASCADE`).
> Only run it on an empty database. There is no process that wipes
> the database: destructive reset was removed on purpose.

Demo accounts (development seed data, passwords never published):
`admin@technova.com` (ADMIN), `tecnico1@technova.com` (TECHNICIAN),
`usuario1@technova.com` (USER). Configure local passwords with
environment variables (see the root README, "Local credentials").

## Configuration

Development project: every value (database, JWT secret, ADMIN/TECHNICIAN
accounts) is fixed in `src/main/resources/application.properties`. Only
`DB_URL` is overridden, by `docker-compose.yml`, to reach the `postgres` host.

`../database/schema.sql` is the single source of truth for the schema; Hibernate only
validates it on startup (`ddl-auto=validate`) and never modifies it.
ADMIN/TECHNICIAN accounts are only created when their email and password
are defined (public registration always creates USER). Any dev-only secret
is local-demo only; any network use needs a random 32+ secret or
the boot fails loudly (see `StartupValidator`).

## Running

From IntelliJ IDEA: open `backend/` as a Maven project, set the
environment variables in the Run configuration and run
`com.technova.campusdesk.CampusDeskApplication`.

Or with Maven Wrapper (no global Maven install):

```bash
cd backend
mvnw.cmd spring-boot:run
```

- API: `http://localhost:8080/api`
- Health: `GET http://localhost:8080/api/system/status` (public)
- Demo seed: `POST http://localhost:8080/api/system/seed` (ADMIN, non-destructive)
- Swagger: `http://localhost:8080/swagger-ui.html` (log in via `POST /api/auth/login`,
  press **Authorize** and paste the token).

## Key rules (summary)

- Public registration → always `USER`. No role field in the input.
- Tickets are born `ABIERTA` with no technician. Only `USER` creates.
- Only `ADMIN` assigns/reassigns, on `ABIERTA` or `ASIGNADA` tickets, to a `TECHNICIAN`.
- Assigned technician: `ASIGNADA → EN_PROCESO → RESUELTA`. Requester: `RESUELTA → CERRADA`
  (optional `rating` 1-5 and `feedback`) or reopen `RESUELTA → EN_PROCESO` (required `comment`).
- Requester only: cancel `ABIERTA`/`ASIGNADA → CANCELADA` (required `comment`, 5-500 chars,
  saved in `cancelReason`).
- SLA: `dueAt` = creation + priority hours (4/24/72/120). `overdue` while not RESUELTA/CERRADA/CANCELADA.
- `RESUELTA` tickets are auto-closed after `AUTO_CLOSE_DAYS` (default 7) by `AutoCloseJob`.
- Internal comments (`internal: true`): only TECHNICIAN/ADMIN write or read them.
- Only the requester edits, on `ABIERTA` tickets with no technician.
- `CERRADA` and `CANCELADA` are final and accept no comments.
- `GET /api/tickets/search` → paged results with `q` (text or `#id`) and `overdue` filters.
- Uniform JSON errors: 400 data, 401 no token, 403 no permission, 404 missing,
  409 business rule, 500 unexpected. No stack traces or secrets.
