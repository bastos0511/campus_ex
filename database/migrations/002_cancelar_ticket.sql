-- =====================================================================
-- Migracion 002 — Estado CANCELADA (cancelar ticket)
--
-- Usala SOLO si tu base ya existia antes de estos cambios (no borra datos).
-- Si creas la base desde cero, schema.sql ya incluye estos cambios.
--
--   psql -U campusdesk_app -d technova_db -f database/migrations/002_cancelar_ticket.sql
-- =====================================================================

BEGIN;

-- Tickets: motivo de cancelacion
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500);

-- Nuevo estado en los CHECK de estados
ALTER TABLE tickets DROP CONSTRAINT IF EXISTS ck_tickets_status;
ALTER TABLE tickets ADD CONSTRAINT ck_tickets_status CHECK (status IN
    ('ABIERTA', 'ASIGNADA', 'EN_PROCESO', 'RESUELTA', 'CERRADA', 'CANCELADA'));

ALTER TABLE ticket_history DROP CONSTRAINT IF EXISTS ck_history_previous;
ALTER TABLE ticket_history ADD CONSTRAINT ck_history_previous CHECK (
    previous_status IS NULL OR previous_status IN
    ('ABIERTA', 'ASIGNADA', 'EN_PROCESO', 'RESUELTA', 'CERRADA', 'CANCELADA'));
ALTER TABLE ticket_history DROP CONSTRAINT IF EXISTS ck_history_new;
ALTER TABLE ticket_history ADD CONSTRAINT ck_history_new CHECK (new_status IN
    ('ABIERTA', 'ASIGNADA', 'EN_PROCESO', 'RESUELTA', 'CERRADA', 'CANCELADA'));

-- CANCELADA puede tener o no tecnico (viene de ABIERTA o ASIGNADA)
ALTER TABLE tickets DROP CONSTRAINT IF EXISTS ck_tickets_status_technician;
ALTER TABLE tickets ADD CONSTRAINT ck_tickets_status_technician CHECK (
    (status = 'ABIERTA' AND assigned_technician_id IS NULL)
    OR status = 'CANCELADA'
    OR (status NOT IN ('ABIERTA', 'CANCELADA') AND assigned_technician_id IS NOT NULL));

-- Solo un ticket CANCELADA tiene motivo, y siempre lo tiene
ALTER TABLE tickets DROP CONSTRAINT IF EXISTS ck_tickets_cancel_reason;
ALTER TABLE tickets ADD CONSTRAINT ck_tickets_cancel_reason CHECK (
    (status = 'CANCELADA' AND cancel_reason IS NOT NULL AND length(trim(cancel_reason)) >= 5)
    OR (status <> 'CANCELADA' AND cancel_reason IS NULL));

-- CANCELADA tambien es estado final: no se modifica ni admite comentarios
CREATE OR REPLACE FUNCTION fn_block_closed_ticket() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IN ('CERRADA', 'CANCELADA') THEN
        RAISE EXCEPTION 'El ticket % está % y no puede modificarse', OLD.id, OLD.status
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_block_comment_closed() RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM tickets WHERE id = NEW.ticket_id
               AND status IN ('CERRADA', 'CANCELADA')) THEN
        RAISE EXCEPTION 'No se pueden agregar comentarios al ticket % (estado final)', NEW.ticket_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Vista de estadisticas con el nuevo estado (se agrega la columna al final)
CREATE OR REPLACE VIEW v_ticket_statistics AS
SELECT
    COUNT(*)                                             AS total,
    COUNT(*) FILTER (WHERE status = 'ABIERTA')           AS abiertas,
    COUNT(*) FILTER (WHERE status = 'ASIGNADA')          AS asignadas,
    COUNT(*) FILTER (WHERE status = 'EN_PROCESO')        AS en_proceso,
    COUNT(*) FILTER (WHERE status = 'RESUELTA')          AS resueltas,
    COUNT(*) FILTER (WHERE status = 'CERRADA')           AS cerradas,
    COUNT(*) FILTER (WHERE status = 'CANCELADA')         AS canceladas
FROM tickets;

COMMIT;
