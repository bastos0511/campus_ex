/* ============================================================
   CampusDesk · js/ui.js
   Helpers reutilizables de interfaz. Evita duplicar código.
   ============================================================ */
"use strict";

const ui = (() => {
  function el(id) { return document.getElementById(id); }

  function showAlert(targetId, message, type = "error") {
    const box = typeof targetId === "string" ? el(targetId) : targetId;
    if (!box) return;
    box.textContent = message;
    box.className = `alert alert-${type} show`;
  }

  function hideAlert(targetId) {
    const box = typeof targetId === "string" ? el(targetId) : targetId;
    if (!box) return;
    box.className = "alert";
    box.textContent = "";
  }

  function setLoading(button, isLoading, label = "Cargando…") {
    if (!button) return;
    if (isLoading) {
      button.dataset.originalText = button.innerHTML;
      button.disabled = true;
      button.innerHTML = `<span class="spinner" aria-hidden="true"></span> ${label}`;
    } else {
      button.disabled = false;
      if (button.dataset.originalText) button.innerHTML = button.dataset.originalText;
    }
  }

  function statusBadge(estado) {
    const safe = String(estado || "").toUpperCase();
    return `<span class="badge st-${safe}">${safe.replace("_", " ")}</span>`;
  }

  function priorityBadge(prioridad) {
    const safe = String(prioridad || "").toUpperCase();
    return `<span class="badge pr-${safe}">${safe}</span>`;
  }

  function categoryBadge(categoria) {
    return `<span class="badge cat">${String(categoria || "").toUpperCase()}</span>`;
  }

  // Fecha con hora (para el SLA, donde las horas importan).
  function formatDateTime(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return String(iso);
    return d.toLocaleString("es-ES", { year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
  }

  // Etiqueta "Vencido" cuando el ticket pasó su fecha límite (SLA).
  function slaBadge(vencido) {
    return vencido ? `<span class="badge sla-vencido" title="Superó el tiempo de atención de su prioridad">Vencido</span>` : "";
  }

  // Calificación 1–5 como estrellas (texto accesible incluido).
  function ratingStars(n) {
    const v = Math.max(0, Math.min(5, Number(n) || 0));
    return `<span class="rating-stars" aria-label="${v} de 5">${"★".repeat(v)}<span class="off">${"★".repeat(5 - v)}</span></span>`;
  }

  function formatDate(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return String(iso);
    return d.toLocaleDateString("es-ES", { year: "numeric", month: "short", day: "numeric" });
  }

  function openModal(id) { el(id)?.classList.add("open"); }
  function closeModal(id) { el(id)?.classList.remove("open"); }

  // Pausa para simular latencia de red en acciones demo (sin backend).
  function delay(ms) { return new Promise((resolve) => setTimeout(resolve, ms)); }

  // Inserta texto dinámico sin interpretarlo como HTML (anti-XSS).
  // Úsala siempre que se rendericen datos (hoy demo, mañana API).
  function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, (c) => (
      { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]
    ));
  }

  // Iniciales para mini-avatares (ej. "María López" → "ML").
  function initials(name) {
    return String(name || "").trim().split(/\s+/).map((w) => w[0]).filter(Boolean)
      .slice(0, 2).join("").toUpperCase() || "–";
  }

  // Técnico asignado o etiqueta sutil de "Sin asignar".
  function techLabel(name) {
    const clean = String(name || "").trim();
    if (!clean || clean.toLowerCase() === "sin asignar") {
      return `<span class="unassigned">Sin asignar</span>`;
    }
    return escapeHtml(clean);
  }

  // Autor con mini-avatar e iniciales en sub-etiqueta sutil.
  function authorCell(name) {
    const clean = String(name || "").trim() || "—";
    return `<span class="cell-user"><span class="mini-avatar" aria-hidden="true">${escapeHtml(initials(clean))}</span><span class="cell-sub">${escapeHtml(clean)}</span></span>`;
  }

  // Tarjeta de ticket tipo etiqueta (franja de prioridad + contenido + lateral).
  // opts controla qué campos se muestran; mismos datos y rutas siempre.
  function ticketCard(t, opts = {}) {
    const { showDate = true, showAuthor = true, showCategory = true, showTech = true, compact = false } = opts;
    const meta = (showAuthor || showCategory)
      ? `<p class="ticket-meta">${showAuthor ? authorCell(t.solicitante) : ""}${showCategory ? categoryBadge(t.categoria) : ""}</p>`
      : "";
    return `
  <article class="ticket-card${compact ? " compact" : ""}" data-pr="${escapeHtml(t.prioridad)}">
    <div class="ticket-strip" aria-hidden="true"><span>${escapeHtml(t.prioridad)}</span></div>
    <div class="ticket-body">
      <p class="ticket-id">${escapeHtml(t.id)}${showDate ? ` · ${formatDate(t.fecha)}` : ""}</p>
      <h3 class="ticket-title">${escapeHtml(t.titulo)}</h3>
      ${meta}
    </div>
    <div class="ticket-side">
      <div class="ticket-badges">${slaBadge(t.vencido)}${statusBadge(t.estado)}</div>
      <div class="ticket-assign">
        ${showTech ? `<p class="ticket-tech">${techLabel(t.tecnico)}</p>` : ""}
        <a class="btn btn-icon" href="ticket-detalle.html?id=${encodeURIComponent(t.id)}" aria-label="Ver detalle del ticket ${escapeHtml(t.id)}">→</a>
      </div>
    </div>
  </article>`;
  }

  // --- Navegación / rol (SOLO visual) ---
  // La seguridad real la hará el backend. El frontend únicamente
  // adapta la interfaz al rol recibido tras la autenticación.
  // Con sesión activa se usa el rol real del JWT; sin sesión,
  // ?role=USER|TECHNICIAN|ADMIN permite previsualizar cada experiencia.
  function getPreviewRole() {
    try {
      const sessionRole = window.CampusDeskApi?.getSessionUser()?.role;
      if (["USER", "TECHNICIAN", "ADMIN"].includes(sessionRole)) return sessionRole;
    } catch { /* sin sesión: se usa el parámetro */ }
    const params = new URLSearchParams(location.search);
    const r = (params.get("role") || "USER").toUpperCase();
    return ["USER", "TECHNICIAN", "ADMIN"].includes(r) ? r : "USER";
  }

  function applyRoleVisibility() {
    const role = getPreviewRole();
    document.querySelectorAll("[data-role-chip]").forEach((n) => { n.textContent = role; });
    // Nombre del usuario de la sesión (si la hay) en el lateral.
    try {
      const me = window.CampusDeskApi?.getSessionUser();
      if (me) {
        document.querySelectorAll("[data-session-name]").forEach((n) => { n.textContent = me.fullName || me.email; });
        document.querySelectorAll("[data-session-email]").forEach((n) => { n.textContent = me.email || ""; });
        document.querySelectorAll("[data-session-avatar]").forEach((n) => { n.textContent = initials(me.fullName || me.email); });
      }
    } catch { /* sin sesión */ }
    document.querySelectorAll("[data-visible-for]").forEach((node) => {
      const allowed = node.dataset.visibleFor.split(",").map((s) => s.trim().toUpperCase());
      const showAll = allowed.includes("ALL");
      node.style.display = showAll || allowed.includes(role) ? "" : "none";
    });
    // Marcar enlace activo por página actual (ignora parámetros como ?role=).
    const page = location.pathname.split("/").pop();
    document.querySelectorAll(".nav-link").forEach((a) => {
      const target = (a.getAttribute("href") || "").split("?")[0].split("#")[0];
      if (target && target.endsWith(page)) a.classList.add("active");
    });
    return role;
  }

  function initSidebar() {
    const btn = el("menuBtn");
    const app = document.querySelector(".app");
    if (!btn || !app) return;
    const sync = () => btn.setAttribute("aria-expanded", String(app.classList.contains("sidebar-open")));
    btn.addEventListener("click", () => { app.classList.toggle("sidebar-open"); sync(); });
    sync();
  }

  // Convierte un ticket de la API al modelo que usan las tarjetas.
  function normalizeTicket(t) {
    return {
      id: t.id,
      titulo: t.title,
      descripcion: t.description,
      categoria: t.categoria || t.category,
      prioridad: t.priority,
      estado: t.status,
      solicitante: t.requester?.fullName || t.solicitante || "—",
      tecnico: t.technician?.fullName || t.tecnico || "Sin asignar",
      fecha: t.createdAt || t.fecha,
      vence: t.dueAt || null,
      vencido: !!t.overdue,
      calificacion: t.rating ?? null,
      opinion: t.feedback || "",
    };
  }

  // Convierte un comentario de la API al modelo de la vista.
  function normalizeComment(c) {
    return {
      autor: c.author?.fullName || c.autor,
      fecha: c.createdAt || c.fecha,
      contenido: c.content ?? c.contenido,
      interno: !!c.internal,
    };
  }

  // Cierra sesión: borra el JWT y vuelve al login.
  // Usa *= porque preserveRoleInLinks añade ?role= al href.
  function initLogout() {
    document.querySelectorAll('nav[aria-label="Principal"] .nav-link[href*="login.html"]')
      .forEach((a) => a.addEventListener("click", (e) => {
        e.preventDefault();
        try { window.CampusDeskApi?.clearSession(); } catch { /* nada */ }
        location.href = "login.html";
      }));
  }
  // Fecha actual en los títulos de página (solo presentación).
  function pageDate() {
    const text = new Date().toLocaleDateString("es-ES", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
    document.querySelectorAll("[data-page-date]").forEach((n) => { n.textContent = text; });
  }
  // Conserva ?role= al navegar entre páginas internas para no perder
  // la vista previa. Respeta otros parámetros existentes (ej. ?id=).
  // Solo presentación: no protege páginas ni datos (eso lo hará el backend).
  function preserveRoleInLinks() {
    const role = getPreviewRole();
    const isFile = location.protocol === "file:";
    document.querySelectorAll("a[href]").forEach((a) => {
      const raw = a.getAttribute("href");
      if (!raw || raw.startsWith("#") || /^[a-z]+:/i.test(raw) || !/\.html(\?|#|$)/.test(raw)) return;
      try {
        const url = new URL(raw, location.href);
        if (!isFile && url.origin !== location.origin) return;
        if (!url.searchParams.get("role")) url.searchParams.set("role", role);
        a.setAttribute("href", isFile ? url.href : url.pathname + url.search + url.hash);
      } catch { /* enlace no válido: se deja intacto */ }
    });
  }

  return {
    el, showAlert, hideAlert, setLoading, delay,
    statusBadge, priorityBadge, categoryBadge, slaBadge, ratingStars,
    formatDate, formatDateTime, openModal, closeModal, escapeHtml, initials, techLabel, authorCell, ticketCard,
    getPreviewRole, applyRoleVisibility, initSidebar, preserveRoleInLinks, pageDate,
    normalizeTicket, normalizeComment, initLogout,
  };
})();

window.CampusDeskUI = ui;

// Decoración automática de enlaces en todas las páginas (sin backend).
document.addEventListener("DOMContentLoaded", () => {
  try { window.CampusDeskUI.preserveRoleInLinks(); } catch { /* no crítico */ }
  try { window.CampusDeskUI.pageDate(); } catch { /* no crítico */ }
});
