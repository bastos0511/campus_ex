-- =====================================================================
-- TechNova Solutions — Technology Incident Management System
-- Database schema (PostgreSQL 13+)
--
-- Usage:
--   createdb -U postgres technova_db
--   psql -U postgres -d technova_db -f schema.sql
--   psql -U postgres -d technova_db -f seed.sql
--
-- Note: enums are stored as VARCHAR with CHECK, so they work
-- directly with @Enumerated(EnumType.STRING) in JPA.
-- With spring.jpa.hibernate.ddl-auto=validate, names match
-- Spring Boot's snake_case convention.
-- =====================================================================

BEGIN;

-- Cleanup (allows re-running the script in development)
DROP TABLE IF EXISTS ticket_history CASCADE;
DROP TABLE IF EXISTS comments       CASCADE;
DROP TABLE IF EXISTS tickets        CASCADE;
DROP TABLE IF EXISTS users          CASCADE;

-- ---------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(100) NOT NULL,           -- BCrypt hash (60 characters)
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role  CHECK (role IN ('ADMIN', 'TECHNICIAN', 'USER')),
    CONSTRAINT ck_users_name  CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_users_email CHECK (email ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$')
);

-- Case-insensitive unique emails
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
CREATE INDEX idx_users_role ON users (role);

-- ---------------------------------------------------------------------
-- TICKETS
-- ---------------------------------------------------------------------
CREATE TABLE tickets (
    id                       BIGSERIAL    PRIMARY KEY,
    title                    VARCHAR(150) NOT NULL,
    description              TEXT         NOT NULL,
    category                 VARCHAR(20)  NOT NULL,
    priority                 VARCHAR(20)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'ABIERTA',
    requester_id             BIGINT       NOT NULL,
    assigned_technician_id   BIGINT,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_at                   TIMESTAMP,                -- SLA: fecha limite segun la prioridad
    rating                   SMALLINT,                 -- calificacion 1-5 al cerrar
    feedback                 VARCHAR(500),             -- comentario de la calificacion

    CONSTRAINT fk_tickets_requester
        FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_tickets_technician
        FOREIGN KEY (assigned_technician_id) REFERENCES users (id) ON DELETE RESTRICT,

    CONSTRAINT ck_tickets_category CHECK (category IN
        ('HARDWARE', 'SOFTWARE', 'REDES', 'ACCESOS', 'OTROS')),
    CONSTRAINT ck_tickets_priority CHECK (priority IN
        ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_tickets_status   CHECK (status IN
        ('ABIERTA', 'ASIGNADA', 'EN_PROCESO', 'RESUELTA', 'CERRADA')),
    CONSTRAINT ck_tickets_title        CHECK (length(trim(title)) >= 5),
    CONSTRAINT ck_tickets_description  CHECK (length(trim(description)) >= 10),

    -- Consistency between status and assigned technician
    CONSTRAINT ck_tickets_status_technician CHECK (
        (status = 'ABIERTA' AND assigned_technician_id IS NULL)
        OR (status <> 'ABIERTA' AND assigned_technician_id IS NOT NULL)
    ),
    CONSTRAINT ck_tickets_dates CHECK (updated_at >= created_at),
    CONSTRAINT ck_tickets_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
    -- Solo un ticket CERRADA puede tener calificacion
    CONSTRAINT ck_tickets_rating_closed CHECK (
        (rating IS NULL AND feedback IS NULL) OR status = 'CERRADA')
);

CREATE INDEX idx_tickets_requester   ON tickets (requester_id);
CREATE INDEX idx_tickets_technician  ON tickets (assigned_technician_id);
CREATE INDEX idx_tickets_status      ON tickets (status);
CREATE INDEX idx_tickets_priority    ON tickets (priority);
CREATE INDEX idx_tickets_category    ON tickets (category);
CREATE INDEX idx_tickets_created_at  ON tickets (created_at DESC);
CREATE INDEX idx_tickets_due_at      ON tickets (due_at);

-- ---------------------------------------------------------------------
-- COMMENTS
-- ---------------------------------------------------------------------
CREATE TABLE comments (
    id          BIGSERIAL   PRIMARY KEY,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author_id   BIGINT      NOT NULL,
    ticket_id   BIGINT      NOT NULL,
    internal    BOOLEAN     NOT NULL DEFAULT FALSE,   -- nota interna: el solicitante no la ve

    CONSTRAINT fk_comments_author
        FOREIGN KEY (author_id) REFERENCES users (id)   ON DELETE RESTRICT,
    CONSTRAINT fk_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    CONSTRAINT ck_comments_content CHECK (length(trim(content)) > 0)
);

CREATE INDEX idx_comments_ticket_created ON comments (ticket_id, created_at ASC);
CREATE INDEX idx_comments_author         ON comments (author_id);

-- ---------------------------------------------------------------------
-- TICKET_HISTORY  (status change history)
-- previous_status is NULL only on the ticket creation record.
-- ---------------------------------------------------------------------
CREATE TABLE ticket_history (
    id               BIGSERIAL   PRIMARY KEY,
    previous_status  VARCHAR(20),
    new_status       VARCHAR(20) NOT NULL,
    changed_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by_id    BIGINT      NOT NULL,
    ticket_id        BIGINT      NOT NULL,

    CONSTRAINT fk_history_changed_by
        FOREIGN KEY (changed_by_id) REFERENCES users (id)   ON DELETE RESTRICT,
    CONSTRAINT fk_history_ticket
        FOREIGN KEY (ticket_id)     REFERENCES tickets (id) ON DELETE CASCADE,

    CONSTRAINT ck_history_previous CHECK (previous_status IS NULL OR previous_status IN
        ('ABIERTA', 'ASIGNADA', 'EN_PROCESO', 'RESUELTA', 'CERRADA')),
    CONSTRAINT ck_history_new CHECK (new_status IN
        ('ABIERTA', 'ASIGNADA', 'EN_PROCESO', 'RESUELTA', 'CERRADA'))
);

CREATE INDEX idx_history_ticket_changed ON ticket_history (ticket_id, changed_at ASC);
CREATE INDEX idx_history_changed_by     ON ticket_history (changed_by_id);

-- ---------------------------------------------------------------------
-- INTEGRITY TRIGGERS
-- ---------------------------------------------------------------------

-- 1) Auto-updates updated_at
CREATE OR REPLACE FUNCTION fn_set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tickets_updated_at
    BEFORE UPDATE ON tickets
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- 2) Only TECHNICIAN users can be assigned technicians
CREATE OR REPLACE FUNCTION fn_check_technician_role() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.assigned_technician_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM users WHERE id = NEW.assigned_technician_id AND role = 'TECHNICIAN'
    ) THEN
        RAISE EXCEPTION 'El usuario % no tiene rol TECHNICIAN', NEW.assigned_technician_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tickets_technician_role
    BEFORE INSERT OR UPDATE OF assigned_technician_id ON tickets
    FOR EACH ROW EXECUTE FUNCTION fn_check_technician_role();

-- 3) CERRADA tickets cannot be modified
CREATE OR REPLACE FUNCTION fn_block_closed_ticket() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = 'CERRADA' THEN
        RAISE EXCEPTION 'El ticket % está CERRADA y no puede modificarse', OLD.id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tickets_block_closed
    BEFORE UPDATE ON tickets
    FOR EACH ROW EXECUTE FUNCTION fn_block_closed_ticket();

-- 4) No comments on CERRADA tickets
CREATE OR REPLACE FUNCTION fn_block_comment_closed() RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM tickets WHERE id = NEW.ticket_id AND status = 'CERRADA') THEN
        RAISE EXCEPTION 'No se pueden agregar comentarios al ticket % (CERRADA)', NEW.ticket_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_comments_block_closed
    BEFORE INSERT ON comments
    FOR EACH ROW EXECUTE FUNCTION fn_block_comment_closed();

-- 5) History is immutable (inserts only)
CREATE OR REPLACE FUNCTION fn_history_immutable() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'El historial de tickets no puede modificarse ni eliminarse manualmente'
        USING ERRCODE = 'restrict_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_history_no_update
    BEFORE UPDATE ON ticket_history
    FOR EACH ROW EXECUTE FUNCTION fn_history_immutable();

-- Direct deletes are blocked, but cascading deletes are allowed
-- when the parent ticket is removed (the ticket no longer exists).
CREATE OR REPLACE FUNCTION fn_history_no_delete() RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM tickets WHERE id = OLD.ticket_id) THEN
        RETURN OLD;  -- cascade from tickets
    END IF;
    RAISE EXCEPTION 'El historial de tickets no puede modificarse ni eliminarse manualmente'
        USING ERRCODE = 'restrict_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_history_no_delete
    BEFORE DELETE ON ticket_history
    FOR EACH ROW EXECUTE FUNCTION fn_history_no_delete();

-- ---------------------------------------------------------------------
-- STATISTICS VIEW (optional, useful for /api/statistics)
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_ticket_statistics AS
SELECT
    COUNT(*)                                             AS total,
    COUNT(*) FILTER (WHERE status = 'ABIERTA')           AS abiertas,
    COUNT(*) FILTER (WHERE status = 'ASIGNADA')          AS asignadas,
    COUNT(*) FILTER (WHERE status = 'EN_PROCESO')        AS en_proceso,
    COUNT(*) FILTER (WHERE status = 'RESUELTA')          AS resueltas,
    COUNT(*) FILTER (WHERE status = 'CERRADA')           AS cerradas
FROM tickets;

COMMIT;
