-- =====================================================================
-- TechNova Solutions — Test data
-- Run AFTER schema.sql.
--
-- TEST ACCOUNTS (development / demo only, roles):
--   admin@technova.com      (ADMIN)
--   tecnico1@technova.com   (TECHNICIAN)
--   tecnico2@technova.com   (TECHNICIAN)
--   usuario1@technova.com   (USER)
--   usuario2@technova.com   (USER)
--
-- Passwords below are stored as BCrypt hashes, never in plain text.
-- Demo passwords are NOT published: configure local credentials with
-- environment variables (see README.md, "Local credentials").
-- =====================================================================

BEGIN;

INSERT INTO users (name, email, password, role, created_at) VALUES
 ('Administrador TechNova', 'admin@technova.com',    '$2a$10$Z2Q2GGdsDcISAx8wPqUa6u0WXzAaaPA15wpo.O7NkNeMgMD4qWsoe', 'ADMIN',      NOW() - INTERVAL '30 days'),
 ('Carlos Técnico',         'tecnico1@technova.com', '$2a$10$bcADH637/jBcgfDwELZBmuZH8vQlmn9PFr9kx/Qz6NcqnvPrd0mhi', 'TECHNICIAN', NOW() - INTERVAL '30 days'),
 ('Laura Técnica',          'tecnico2@technova.com', '$2a$10$bcADH637/jBcgfDwELZBmuZH8vQlmn9PFr9kx/Qz6NcqnvPrd0mhi', 'TECHNICIAN', NOW() - INTERVAL '30 days'),
 ('Ana Gómez',              'usuario1@technova.com', '$2a$10$oRXrGXIXuLHE/Otg82z9G.OX5lvHGVoFJbhDH3OmW6wEzG9YJ97yu', 'USER',       NOW() - INTERVAL '20 days'),
 ('Pedro Ramírez',          'usuario2@technova.com', '$2a$10$oRXrGXIXuLHE/Otg82z9G.OX5lvHGVoFJbhDH3OmW6wEzG9YJ97yu', 'USER',       NOW() - INTERVAL '20 days');

-- ---------------------------------------------------------------------
-- Tickets across lifecycle states
-- ---------------------------------------------------------------------
INSERT INTO tickets (title, description, category, priority, status, requester_id, assigned_technician_id, created_at, updated_at, due_at) VALUES
 -- 1) ABIERTA
 ('No enciende el monitor',
  'El monitor de mi puesto de trabajo no enciende desde esta mañana.',
  'HARDWARE', 'MEDIA', 'ABIERTA',
  (SELECT id FROM users WHERE email='usuario1@technova.com'), NULL,
  NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days',
  NOW() - INTERVAL '2 days' + INTERVAL '72 hours'),
 -- 2) ASIGNADA
 ('Sin acceso a la VPN corporativa',
  'No puedo conectarme a la VPN desde mi casa, aparece error de autenticación.',
  'REDES', 'ALTA', 'ASIGNADA',
  (SELECT id FROM users WHERE email='usuario2@technova.com'),
  (SELECT id FROM users WHERE email='tecnico1@technova.com'),
  NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days',
  NOW() - INTERVAL '5 days' + INTERVAL '24 hours'),
 -- 3) EN_PROCESO
 ('Instalación de Visual Studio Code',
  'Necesito instalar Visual Studio Code y el JDK 21 en mi equipo.',
  'SOFTWARE', 'BAJA', 'EN_PROCESO',
  (SELECT id FROM users WHERE email='usuario1@technova.com'),
  (SELECT id FROM users WHERE email='tecnico2@technova.com'),
  NOW() - INTERVAL '6 days', NOW() - INTERVAL '3 days',
  NOW() - INTERVAL '6 days' + INTERVAL '120 hours'),
 -- 4) RESUELTA
 ('Restablecer acceso al sistema de nómina',
  'Mi usuario del sistema de nómina aparece bloqueado.',
  'ACCESOS', 'CRITICA', 'RESUELTA',
  (SELECT id FROM users WHERE email='usuario2@technova.com'),
  (SELECT id FROM users WHERE email='tecnico1@technova.com'),
  NOW() - INTERVAL '8 days', NOW() - INTERVAL '6 days',
  NOW() - INTERVAL '8 days' + INTERVAL '4 hours'),
 -- 5) CERRADA
 ('Teclado con teclas defectuosas',
  'Varias teclas del teclado no responden correctamente.',
  'HARDWARE', 'BAJA', 'CERRADA',
  (SELECT id FROM users WHERE email='usuario1@technova.com'),
  (SELECT id FROM users WHERE email='tecnico2@technova.com'),
  NOW() - INTERVAL '15 days', NOW() - INTERVAL '10 days',
  NOW() - INTERVAL '15 days' + INTERVAL '120 hours');

-- ---------------------------------------------------------------------
-- History (matching each ticket's current status)
-- ---------------------------------------------------------------------
INSERT INTO ticket_history (previous_status, new_status, changed_at, changed_by_id, ticket_id)
SELECT v.prev, v.new, NOW() - v.ago, u.id, t.id
FROM (VALUES
 -- ticket 1
 ('No enciende el monitor',                    NULL,         'ABIERTA',    INTERVAL '2 days',  'usuario1@technova.com'),
 -- ticket 2
 ('Sin acceso a la VPN corporativa',           NULL,         'ABIERTA',    INTERVAL '5 days',  'usuario2@technova.com'),
 ('Sin acceso a la VPN corporativa',           'ABIERTA',    'ASIGNADA',   INTERVAL '4 days',  'admin@technova.com'),
 -- ticket 3
 ('Instalación de Visual Studio Code',         NULL,         'ABIERTA',    INTERVAL '6 days',  'usuario1@technova.com'),
 ('Instalación de Visual Studio Code',         'ABIERTA',    'ASIGNADA',   INTERVAL '5 days',  'admin@technova.com'),
 ('Instalación de Visual Studio Code',         'ASIGNADA',   'EN_PROCESO', INTERVAL '3 days',  'tecnico2@technova.com'),
 -- ticket 4
 ('Restablecer acceso al sistema de nómina',   NULL,         'ABIERTA',    INTERVAL '8 days',  'usuario2@technova.com'),
 ('Restablecer acceso al sistema de nómina',   'ABIERTA',    'ASIGNADA',   INTERVAL '8 days' - INTERVAL '1 hour', 'admin@technova.com'),
 ('Restablecer acceso al sistema de nómina',   'ASIGNADA',   'EN_PROCESO', INTERVAL '7 days',  'tecnico1@technova.com'),
 ('Restablecer acceso al sistema de nómina',   'EN_PROCESO', 'RESUELTA',   INTERVAL '6 days',  'tecnico1@technova.com'),
 -- ticket 5
 ('Teclado con teclas defectuosas',            NULL,         'ABIERTA',    INTERVAL '15 days', 'usuario1@technova.com'),
 ('Teclado con teclas defectuosas',            'ABIERTA',    'ASIGNADA',   INTERVAL '14 days', 'admin@technova.com'),
 ('Teclado con teclas defectuosas',            'ASIGNADA',   'EN_PROCESO', INTERVAL '13 days', 'tecnico2@technova.com'),
 ('Teclado con teclas defectuosas',            'EN_PROCESO', 'RESUELTA',   INTERVAL '11 days', 'tecnico2@technova.com'),
 ('Teclado con teclas defectuosas',            'RESUELTA',   'CERRADA',    INTERVAL '10 days', 'usuario1@technova.com')
) AS v(title, prev, new, ago, email)
JOIN tickets t ON t.title = v.title
JOIN users   u ON u.email = v.email;

-- ---------------------------------------------------------------------
-- Comments
-- (the trigger blocking comments on CERRADA tickets is disabled
--  only to load the historical comment of the already-closed ticket)
-- ---------------------------------------------------------------------
ALTER TABLE comments DISABLE TRIGGER trg_comments_block_closed;

INSERT INTO comments (content, created_at, author_id, ticket_id)
SELECT v.content, NOW() - v.ago, u.id, t.id
FROM (VALUES
 ('Sin acceso a la VPN corporativa',   'Revisaré tu perfil de VPN y te escribo en breve.',               INTERVAL '4 days',  'tecnico1@technova.com'),
 ('Sin acceso a la VPN corporativa',   'Gracias, quedo atento.',                                          INTERVAL '3 days',  'usuario2@technova.com'),
 ('Instalación de Visual Studio Code', 'Ya inicié la instalación, falta configurar las extensiones.',     INTERVAL '3 days',  'tecnico2@technova.com'),
 ('Restablecer acceso al sistema de nómina', 'Usuario desbloqueado y contraseña restablecida.',           INTERVAL '6 days',  'tecnico1@technova.com'),
 ('Teclado con teclas defectuosas',    'Se reemplazó el teclado por uno nuevo.',                          INTERVAL '11 days', 'tecnico2@technova.com'),
 ('Teclado con teclas defectuosas',    'Todo funciona bien, cierro la solicitud. Gracias.',               INTERVAL '10 days' + INTERVAL '1 minute', 'usuario1@technova.com')
) AS v(title, content, ago, email)
JOIN tickets t ON t.title = v.title
JOIN users   u ON u.email = v.email;

ALTER TABLE comments ENABLE TRIGGER trg_comments_block_closed;

COMMIT;
