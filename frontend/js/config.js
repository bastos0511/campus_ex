/* ============================================================
   CampusDesk · js/config.js
   ÚNICO lugar para la configuración del futuro backend.
   Cuando el equipo backend entregue el contrato real,
   modificar únicamente este archivo (+ js/api.js).
   ============================================================ */
"use strict";

const API_BASE_URL = "http://localhost:8080/api";

// Tiempo de espera y opciones globales para fetch (uso futuro).
const API_CONFIG = {
  baseUrl: API_BASE_URL,
  timeoutMs: 12000,
  // Ejemplo futuro: headers comunes (Authorization se añadirá con JWT).
  // getHeaders: (token) => ({ "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) })
};