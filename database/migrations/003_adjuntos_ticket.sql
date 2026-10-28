BEGIN;

CREATE TABLE IF NOT EXISTS ticket_attachments (
    id                BIGSERIAL     PRIMARY KEY,
    nombre_archivo    VARCHAR(255)  NOT NULL,
    url_o_referencia  VARCHAR(2000) NOT NULL,
    tipo_contenido    VARCHAR(20)   NOT NULL,
    fecha_creacion    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ticket_id         BIGINT        NOT NULL,
    subido_por_id     BIGINT        NOT NULL,

    CONSTRAINT fk_attachments_ticket
        FOREIGN KEY (ticket_id)     REFERENCES tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_attachments_subido_por
        FOREIGN KEY (subido_por_id) REFERENCES users (id)   ON DELETE RESTRICT,

    CONSTRAINT ck_attachments_tipo CHECK (tipo_contenido IN
        ('IMAGEN', 'LOG_TEXTO', 'ENLACE_EXTERNO')),
    CONSTRAINT ck_attachments_nombre CHECK (length(trim(nombre_archivo)) > 0),
    CONSTRAINT ck_attachments_url CHECK (length(trim(url_o_referencia)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_attachments_ticket_fecha
    ON ticket_attachments (ticket_id, fecha_creacion ASC);
CREATE INDEX IF NOT EXISTS idx_attachments_subido_por
    ON ticket_attachments (subido_por_id);

COMMIT;
