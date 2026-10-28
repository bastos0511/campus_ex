/* CampusDesk · js/dashboard.js — métricas y recientes desde la API. */
"use strict";

document.addEventListener("DOMContentLoaded", () => {
  const ui = window.CampusDeskUI;
  const api = window.CampusDeskApi;
  ui.initSidebar();
  ui.initLogout();
  ui.applyRoleVisibility();
  if (!api?.getToken()) {
    location.href = "login.html";
    return;
  }
  loadDashboard();
});

async function loadDashboard() {
  const ui = window.CampusDeskUI;
  const api = window.CampusDeskApi;
  try {
    const [summary, tickets] = await Promise.all([api.getSummary(), api.listTickets()]);
    renderStats(summary);
    renderRecent(tickets.slice(0, 5));
    renderCategories(tickets);
    ui.hideAlert("dashboardAlert");
  } catch (err) {
    console.debug("[CampusDesk] Error en dashboard:", err);
    ui.showAlert("dashboardAlert", err.message || "No se pudo cargar el resumen.", "error");
  }
}

function renderStats(summary) {
  const set = (id, v) => { const n = document.getElementById(id); if (n) n.textContent = v ?? "—"; };
  set("statTotal", summary.total);
  set("statAbiertas", summary.abiertas);
  set("statProceso", summary.enProceso);
  set("statResueltas", summary.resueltas);
  set("statCerradas", summary.cerradas);
}

function renderRecent(tickets) {
  const ui = window.CampusDeskUI;
  const body = document.getElementById("recentBody");
  if (!body) return;
  if (!tickets.length) {
    body.innerHTML = `<div class="empty">No hay tickets para mostrar.</div>`;
    return;
  }
  body.innerHTML = tickets
    .map((t) => ui.ticketCard(ui.normalizeTicket(t), { showDate: false, showTech: false, compact: true }))
    .join("");
  ui.preserveRoleInLinks();
}

// Conteos por categoría calculados de la lista real.
function renderCategories(tickets) {
  const counts = { HARDWARE: 0, SOFTWARE: 0, REDES: 0, ACCESOS: 0, OTROS: 0 };
  tickets.forEach((t) => { if (counts[t.category] !== undefined) counts[t.category] += 1; });
  const list = document.getElementById("categoryCounts");
  if (list) {
    list.innerHTML = Object.entries(counts)
      .map(([cat, n]) => `<li>${cat} <b>${n}</b></li>`)
      .join("");
  }
}
