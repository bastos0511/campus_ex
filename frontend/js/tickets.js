/* CampusDesk · js/tickets.js — listado, filtros y creación contra la API. */
"use strict";

document.addEventListener("DOMContentLoaded", () => {
  const ui = window.CampusDeskUI;
  ui.initSidebar();
  ui.initLogout();
  ui.applyRoleVisibility();
  if (!window.CampusDeskApi?.getToken()) {
    location.href = "login.html";
    return;
  }

  // Listado (pages/tickets.html)
  if (document.getElementById("ticketsBody")) {
    renderTickets();
    // Filtros: vuelven a la página 1. La búsqueda espera a que el usuario
    // deje de escribir (300 ms) para no lanzar una petición por tecla.
    ["filterEstado", "filterPrioridad", "filterCategoria", "filterVencidos"].forEach((id) =>
      document.getElementById(id)?.addEventListener("change", () => goToPage(0))
    );
    let searchTimer = null;
    document.getElementById("searchInput")?.addEventListener("input", () => {
      clearTimeout(searchTimer);
      searchTimer = setTimeout(() => goToPage(0), 300);
    });
    document.getElementById("clearFiltersBtn")?.addEventListener("click", clearFilters);
    document.getElementById("prevPageBtn")?.addEventListener("click", () => goToPage(listState.page - 1));
    document.getElementById("nextPageBtn")?.addEventListener("click", () => goToPage(listState.page + 1));
  }

  // Crear (pages/crear-ticket.html)
  const createForm = document.getElementById("createTicketForm");
  if (createForm) {
    document.getElementById("cancelTicketBtn")?.addEventListener("click", () => history.back());
    createForm.addEventListener("submit", handleCreate);
  }
});

// Página actual del listado (el backend pagina y filtra; aquí solo se muestra).
const PAGE_SIZE = 10;
const listState = { page: 0, totalPages: 1, requestId: 0 };

function currentFilters() {
  const v = (id) => document.getElementById(id)?.value || "";
  return {
    status: v("filterEstado"),
    priority: v("filterPrioridad"),
    category: v("filterCategoria"),
    q: v("searchInput").trim(),
    overdue: document.getElementById("filterVencidos")?.checked ? "true" : "",
  };
}

function goToPage(page) {
  listState.page = Math.max(0, page);
  renderTickets();
}

async function renderTickets() {
  const ui = window.CampusDeskUI;
  const api = window.CampusDeskApi;
  const body = document.getElementById("ticketsBody");
  const count = document.getElementById("ticketsCount");
  const f = currentFilters();
  const hasFilter = !!(f.status || f.priority || f.category || f.q || f.overdue);
  // Si llegan respuestas fuera de orden (búsqueda rápida), solo vale la última.
  const requestId = ++listState.requestId;
  try {
    const result = await api.searchTickets({ ...f, page: listState.page, size: PAGE_SIZE });
    if (requestId !== listState.requestId) return;
    // Si la página quedó vacía (p. ej. tras filtrar), vuelve a la última con datos.
    if (!result.items.length && result.totalPages > 0 && listState.page >= result.totalPages) {
      goToPage(result.totalPages - 1);
      return;
    }
    const rows = result.items.map((t) => ui.normalizeTicket(t));
    if (count) count.textContent = `${result.totalItems} ticket(s)`;
    if (!rows.length) {
      body.innerHTML = `<div class="empty">${
        hasFilter
          ? "Sin resultados con esos filtros. Usa «Limpiar filtros» para ver el listado completo."
          : "No hay tickets para mostrar."
      }</div>`;
    } else {
      body.innerHTML = rows.map((t) => ui.ticketCard(t)).join("");
    }
    renderPager(result);
    // Limpiar filtros visible solo cuando hay algún filtro activo.
    document.getElementById("clearFiltersBtn")?.toggleAttribute("hidden", !hasFilter);
    ui.hideAlert("ticketsAlert");
    ui.preserveRoleInLinks();
  } catch (err) {
    if (requestId !== listState.requestId) return;
    console.debug("[CampusDesk] Error en tickets:", err);
    ui.showAlert("ticketsAlert", err.message || "No se pudo cargar el listado.", "error");
    if (count) count.textContent = "—";
    body.innerHTML = `<div class="empty">Listado no disponible por un error de carga.</div>`;
    document.getElementById("ticketsPager")?.toggleAttribute("hidden", true);
  }
}

// Botones Anterior/Siguiente; se ocultan si todo cabe en una página.
function renderPager(result) {
  const pager = document.getElementById("ticketsPager");
  if (!pager) return;
  listState.totalPages = Math.max(result.totalPages, 1);
  pager.toggleAttribute("hidden", result.totalPages <= 1);
  document.getElementById("pageInfo").textContent = `Página ${result.page + 1} de ${listState.totalPages}`;
  document.getElementById("prevPageBtn").disabled = result.page <= 0;
  document.getElementById("nextPageBtn").disabled = result.page + 1 >= listState.totalPages;
}

function clearFilters() {
  ["filterEstado", "filterPrioridad", "filterCategoria", "searchInput"].forEach((id) => {
    const n = document.getElementById(id); if (n) n.value = "";
  });
  const vencidos = document.getElementById("filterVencidos");
  if (vencidos) vencidos.checked = false;
  goToPage(0);
}

async function handleCreate(e) {
  e.preventDefault();
  const ui = window.CampusDeskUI;
  const api = window.CampusDeskApi;
  ui.hideAlert("createAlert");
  const titulo = document.getElementById("ctTitulo");
  const desc = document.getElementById("ctDescripcion");
  const categoria = document.getElementById("ctCategoria");
  const prioridad = document.getElementById("ctPrioridad");
  let ok = true;
  const tituloOk = titulo.value.trim().length >= 5;
  titulo.closest(".field").classList.toggle("invalid", !tituloOk);
  titulo.setAttribute("aria-invalid", String(!tituloOk));
  ok = tituloOk && ok;
  const descOk = desc.value.trim().length >= 10;
  desc.closest(".field").classList.toggle("invalid", !descOk);
  desc.setAttribute("aria-invalid", String(!descOk));
  ok = descOk && ok;
  if (!ok) return;

  const btn = document.getElementById("createTicketBtn");
  ui.setLoading(btn, true, "Creando…");
  try {
    // El estado inicial (ABIERTA) lo asigna el backend.
    const created = await api.createTicket({
      title: titulo.value.trim(),
      description: desc.value.trim(),
      category: categoria.value,
      priority: prioridad.value,
    });
    location.href = `ticket-detalle.html?id=${created.id}`;
  } catch (err) {
    console.debug("[CampusDesk] Error al crear:", err);
    ui.showAlert("createAlert", err.message || "No se pudo crear el ticket.", "error");
  } finally {
    ui.setLoading(btn, false);
  }
}
