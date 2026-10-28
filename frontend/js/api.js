/* ============================================================
   CampusDesk · js/api.js
   Capa centralizada de acceso a la API.

   Flujo:
     módulo (auth/dashboard/tickets/...) → api.* → fetch()
     → backend (JSON) → frontend

   REGLA: ningún otro archivo debe llamar a fetch() directamente.
   El token JWT se guarda en el navegador y viaja en cada petición
   protegida. Un 401 limpia la sesión y devuelve al login.
   ============================================================ */
"use strict";

const api = (() => {
  const TOKEN_KEY = "campusdesk_token";
  const USER_KEY = "campusdesk_user";

  function baseUrl() {
    // Definida en js/config.js (por defecto http://localhost:8080/api).
    return (typeof API_BASE_URL !== "undefined" ? API_BASE_URL : "http://localhost:8080/api")
      .replace(/\/$/, "");
  }

  function getToken() {
    try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
  }

  function saveSession(token, user) {
    try {
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    } catch { /* sin almacenamiento: la sesión solo vive en memoria */ }
  }

  function getSessionUser() {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch { return null; }
  }

  function clearSession() {
    try { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); } catch { /* nada */ }
  }

  function isAuthPage() {
    return /(login|registro)\.html$/.test(location.pathname);
  }

  // Petición genérica: arma URL, cabeceras y convierte errores ApiError.
  async function request(path, options = {}) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    const token = getToken();
    if (token && !path.startsWith("/auth/")) headers.Authorization = `Bearer ${token}`;
    let res;
    try {
      res = await fetch(`${baseUrl()}${path}`, { ...options, headers });
    } catch {
      throw new Error("No se pudo conectar con el servidor. Revisa que el backend esté en marcha.");
    }
    if (res.status === 401 && !isAuthPage()) {
      clearSession();
      location.href = "login.html";
      throw new Error("Sesión vencida. Inicia sesión de nuevo.");
    }
    if (res.status === 204) return null;
    let body = null;
    try { body = await res.json(); } catch { /* respuesta sin JSON */ }
    if (!res.ok) {
      throw new Error((body && body.message) || `Error del servidor (${res.status}).`);
    }
    return body;
  }

  function query(params) {
    const q = new URLSearchParams();
    Object.entries(params || {}).forEach(([k, v]) => { if (v) q.set(k, v); });
    const s = q.toString();
    return s ? `?${s}` : "";
  }

  return {
    // --- Sesión (solo guarda el JWT, nunca es base de datos) ---
    saveSession, getSessionUser, clearSession, getToken,

    // --- Auth ---
    register(data) { return request("/auth/register", { method: "POST", body: JSON.stringify(data) }); },
    login(data) { return request("/auth/login", { method: "POST", body: JSON.stringify(data) }); },
    getMe() { return request("/auth/me"); },

    // --- Tickets ---
    listTickets(filters) { return request(`/tickets${query(filters)}`); },
    // Búsqueda paginada: { status, priority, category, q, overdue, page, size }.
    // Responde { items, page, size, totalItems, totalPages }.
    searchTickets(params) { return request(`/tickets/search${query(params)}`); },
    getTicket(id) { return request(`/tickets/${id}`); },
    updateTicket(id, payload) { return request(`/tickets/${id}`, { method: "PUT", body: JSON.stringify(payload) }); },
    createTicket(payload) { return request("/tickets", { method: "POST", body: JSON.stringify(payload) }); },
    // extra (opcional): { rating, feedback } al cerrar · { comment } al reabrir.
    changeStatus(id, status, extra = {}) {
      return request(`/tickets/${id}/status`, { method: "PATCH", body: JSON.stringify({ status, ...extra }) });
    },
    getComments(ticketId) { return request(`/tickets/${ticketId}/comments`); },
    // internal=true: nota interna (solo técnicos y administradores la ven).
    addComment(ticketId, content, internal = false) {
      return request(`/tickets/${ticketId}/comments`, {
        method: "POST", body: JSON.stringify({ content, internal }),
      });
    },
    getHistory(ticketId) { return request(`/tickets/${ticketId}/history`); },

    // --- Dashboard ---
    getSummary() { return request("/reports/summary"); },

    // --- Admin ---
    listUsers() { return request("/users"); },
    listTechnicians() { return request("/users/technicians"); },
    assignTechnician(ticketId, technicianId) {
      return request(`/tickets/${ticketId}/assign`, {
        method: "PATCH", body: JSON.stringify({ technicianId }),
      });
    },
  };
})();

window.CampusDeskApi = api;
