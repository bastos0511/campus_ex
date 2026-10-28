-- =====================================================================
-- Migracion 001 — SLA, calificacion al cerrar y notas internas
--
-- Usala SOLO si tu base ya existia antes de estos cambios (no borra datos).
-- Si creas la base desde cero, schema.sql ya incluye estas columnas.
--
--   psql -U campusdesk_app -d technova_db -f database/migrations/001_sla_calificacion_notas_internas.sql
-- =====================================================================

BEGIN;

-- Tickets: fecha limite (SLA) y calificacion del solicitante
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS due_at   TIMESTAMP;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS rating   SMALLINT;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS feedback VARCHAR(500);

ALTER TABLE tickets DROP CONSTRAINT IF EXISTS ck_tickets_rating;
ALTER TABLE tickets ADD CONSTRAINT ck_tickets_rating
    CHECK (rating IS NULL OR rating BETWEEN 1 AND 5);
ALTER TABLE tickets DROP CONSTRAINT IF EXISTS ck_tickets_rating_closed;
ALTER TABLE tickets ADD CONSTRAINT ck_tickets_rating_closed
    CHECK ((rating IS NULL AND feedback IS NULL) OR status = 'CERRADA');

CREATE INDEX IF NOT EXISTS idx_tickets_due_at ON tickets (due_at);

-- Calcula el SLA de los tickets existentes. Se apagan un momento los
-- triggers para no tocar updated_at ni chocar con los tickets CERRADA.
ALTER TABLE tickets DISABLE TRIGGER trg_tickets_updated_at;
ALTER TABLE tickets DISABLE TRIGGER trg_tickets_block_closed;

UPDATE tickets SET due_at = created_at + CASE priority
        WHEN 'CRITICA' THEN INTERVAL '4 hours'
        WHEN 'ALTA'    THEN INTERVAL '24 hours'
        WHEN 'MEDIA'   THEN INTERVAL '72 hours'
        ELSE                INTERVAL '120 hours'
    END
WHERE due_at IS NULL;

ALTER TABLE tickets ENABLE TRIGGER trg_tickets_updated_at;
ALTER TABLE tickets ENABLE TRIGGER trg_tickets_block_closed;

-- Comentarios: notas internas (solo tecnicos y administradores)
ALTER TABLE comments ADD COLUMN IF NOT EXISTS internal BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;
