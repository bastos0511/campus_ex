/* CampusDesk · js/ticket-detalle.js — detalle, comentarios e historial desde la API. */
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
  renderDetail();
  document.getElementById("commentForm")?.addEventListener("submit", handleComment);
  document.getElementById("editForm")?.addEventListener("submit", handleEditSubmit);
  document.getElementById("cancelEditBtn")?.addEventListener("click", () => {
    document.getElementById("editForm").style.display = "none";
  });
  document.getElementById("closeForm")?.addEventListener("submit", handleCloseSubmit);
  document.getElementById("reopenForm")?.addEventListener("submit", handleReopenSubmit);
  document.querySelectorAll("[data-cancel-subform]").forEach((b) =>
    b.addEventListener("click", hideSubforms));
  document.querySelectorAll('#starPicker input[name="rating"]').forEach((r) =>
    r.addEventListener("change", paintStars));
});

const RATING_LABELS = ["Sin calificación", "1 · Muy mala", "2 · Mala", "3 · Aceptable", "4 · Buena", "5 · Excelente"];

// Oculta los formularios de editar, cerrar y reabrir.
function hideSubforms() {
  ["editForm", "closeForm", "reopenForm"].forEach((id) => {
    const f = document.getElementById(id);
    if (f) f.style.display = "none";
  });
}

// Resalta las estrellas hasta la elegida.
function paintStars() {
  const chosen = Number(document.querySelector('#starPicker input[name="rating"]:checked')?.value || 0);
  document.querySelectorAll("#starPicker label").forEach((label, i) => {
    label.classList.toggle("on", i < chosen);
  });
  const text = document.getElementById("ratingText");
  if (text) text.textContent = RATING_LABELS[chosen];
}

function ticketIdFromUrl() {
  return new URLSearchParams(location.search).get("id");
}

async function renderDetail() {
  const ui = window.CampusDeskUI;
  const api = window.CampusDeskApi;
  const content = ui.el("detalleContent");
  const notFound = ui.el("detalleNotFound");
  const showNotFound = () => {
    if (content) content.style.display = "none";
    if (notFound) notFound.style.display = "";
    ui.el("dTitulo").textContent = "Detalle del ticket";
    ui.el("dEstado").innerHTML = "";
  };
  const id = ticketIdFromUrl();
  if (!id) { showNotFound(); return; }
  try {
    const [raw, comments, history] = await Promise.all([
      api.getTicket(id),
      api.getComments(id),
      api.getHistory(id),
    ]);
    const t = ui.normalizeTicket(raw);
    ui.el("dId").textContent = `#${t.id}`;
    ui.el("dTitulo").textContent = t.titulo;
    ui.el("dDescripcion").textContent = t.descripcion;
    ui.el("dCategoria").innerHTML = ui.categoryBadge(t.categoria);
    ui.el("dPrioridad").innerHTML = ui.priorityBadge(t.prioridad);
    ui.el("dEstado").innerHTML = ui.statusBadge(t.estado);
    ui.el("dSolicitante").textContent = t.solicitante;
    ui.el("dTecnico").textContent = t.tecnico;
    ui.el("dFecha").textContent = ui.formatDate(t.fecha);
    ui.el("dVence").innerHTML = t.vence
      ? `${ui.escapeHtml(ui.formatDateTime(t.vence))} ${ui.slaBadge(t.vencido)}`
      : "—";
    const rated = t.calificacion != null;
    ui.el("dCalificacionLabel").style.display = rated ? "" : "none";
    ui.el("dCalificacion").style.display = rated ? "" : "none";
    if (rated) {
      ui.el("dCalificacion").innerHTML = `${ui.ratingStars(t.calificacion)}${
        t.opinion ? `<div class="rating-text">«${ui.escapeHtml(t.opinion)}»</div>` : ""}`;
    }

    const closed = t.estado === "CERRADA";
    ui.el("readonlyNote").style.display = closed ? "" : "none";
    const commentBox = ui.el("commentText");
    const commentBtn = ui.el("commentBtn");
    const internalBox = ui.el("commentInternal");
    if (closed) {
      commentBox.disabled = true; commentBtn.disabled = true;
      if (internalBox) internalBox.disabled = true;
    }

    renderComments(comments.map((c) => ui.normalizeComment(c)));
    renderHistory(history, t.estado);
    renderActions(raw);
  } catch (err) {
    console.debug("[CampusDesk] Error en detalle:", err);
    showNotFound();
  }
}

// Acciones permitidas según rol y estado (el backend valida de nuevo).
function renderActions(raw) {
  const ui = window.CampusDeskUI;
  const me = window.CampusDeskApi?.getSessionUser();
  const card = ui.el("actionsCard");
  const box = ui.el("statusActions");
  const form = ui.el("editForm");
  if (!card || !box || !me) return;
  ui.hideAlert("actionsAlert");
  box.innerHTML = "";
  hideSubforms();

  const isOwner = raw.requester?.id === me.id;
  const isTech = raw.technician?.id === me.id;
  const canEdit = me.role === "USER" && isOwner && raw.status === "ABIERTA" && !raw.technician;
  const nextByTech = me.role === "TECHNICIAN" && isTech
    ? { ASIGNADA: "EN_PROCESO", EN_PROCESO: "RESUELTA" }[raw.status] : null;
  const canClose = me.role === "USER" && isOwner && raw.status === "RESUELTA";
  const canReopen = canClose; // mismo dueño y mismo estado: RESUELTA → EN_PROCESO

  const actions = [];
  if (canEdit) actions.push({ id: "edit", label: "Editar ticket", kind: "secondary" });
  if (nextByTech) actions.push({ id: "status", label: `Marcar ${nextByTech.replace("_", " ")}`, kind: "primary", status: nextByTech });
  if (canClose) actions.push({ id: "close", label: "Cerrar y calificar", kind: "primary" });
  if (canReopen) actions.push({ id: "reopen", label: "No se resolvió: reabrir", kind: "secondary" });
  if (!actions.length) { card.style.display = "none"; return; }

  card.style.display = "";
  actions.forEach((a) => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = `btn btn-${a.kind} btn-sm`;
    btn.textContent = a.label;
    btn.addEventListener("click", () => {
      if (a.id === "edit") openEditForm(raw);
      else if (a.id === "close") openCloseForm();
      else if (a.id === "reopen") openReopenForm();
      else changeTicketStatus(raw.id, a.status, btn);
    });
    box.appendChild(btn);
  });
}

// Muestra el formulario de edición con los datos actuales.
function openEditForm(raw) {
  const ui = window.CampusDeskUI;
  hideSubforms();
  ui.el("editTitulo").value = raw.title || "";
  ui.el("editDescripcion").value = raw.description || "";
  ui.el("editCategoria").value = raw.category || "OTROS";
  ui.el("editPrioridad").value = raw.priority || "MEDIA";
  ui.el("editForm").style.display = "";
  ui.el("editTitulo").focus();
}

async function handleEditSubmit(e) {
  e.preventDefault();
  const ui = window.CampusDeskUI;
  const api = window.CampusDeskApi;
  ui.hideAlert("actionsAlert");
  const titulo = ui.el("editTitulo");
  const desc = ui.el("editDescripcion");
  const tituloOk = titulo.value.trim().length >= 5;
  const descOk = desc.value.trim().length >= 10;
  titulo.closest(".field").classList.toggle("invalid", !tituloOk);
  titulo.setAttribute("aria-invalid", String(!tituloOk));
  desc.closest(".field").classList.toggle("invalid", !descOk);
  desc.setAttribute("aria-invalid", String(!descOk));
  if (!tituloOk || !descOk) return;

  const btn = ui.el("saveEditBtn");
  ui.setLoading(btn, true, "Guardando…");
  try {
    await api.updateTicket(ticketIdFromUrl(), {
      title: titulo.value.trim(),
      description: desc.value.trim(),
      category: ui.el("editCategoria").value,
      priority: ui.el("editPrioridad").value,
    });
    ui.showAlert("actionsAlert", "Ticket actualizado correctamente.", "success");
    renderDetail();
  } catch (err) {
    console.debug("[CampusDesk] Error al editar:", err);
    ui.showAlert("actionsAlert", err.message || "No se pudo guardar.", "error");
  } finally {
    ui.setLoading(btn, false);
  }
}

// Formulario de cierre: estrellas (opcional) + comentario (opcional).
function openCloseForm() {
  hideSubforms();
  document.querySelectorAll('#starPicker input[name="rating"]').forEach((r) => { r.checked = false; });
  document.getElementById("closeFeedback").value = "";
  paintStars();
  document.getElementById("closeForm").style.display = "";
  document.getElementById("star1").focus();
}

function openReopenForm() {
  hideSubforms();
  const reason = document.getElementById("reopenReason");
  reason.value = "";
  reason.closest(".field").classList.remove("invalid");
  reason.setAttribute("aria-invalid", "false");
  document.getElementById("reopenForm").style.display = "";
  reason.focus();
}

async function handleCloseSubmit(e) {
  e.preventDefault();
  const rating = Number(document.querySelector('#starPicker input[name="rating"]:checked')?.value || 0);
  const feedback = document.getElementById("closeFeedback").value.trim();
  const extra = {};
  if (rating) extra.rating = rating;
  if (feedback) extra.feedback = feedback;
  await changeTicketStatus(ticketIdFromUrl(), "CERRADA", document.getElementById("confirmCloseBtn"), extra);
}

async function handleReopenSubmit(e) {
  e.preventDefault();
  const reason = document.getElementById("reopenReason");
  const ok = reason.value.trim().length >= 5;
  reason.closest(".field").classList.toggle("invalid", !ok);
  reason.setAttribute("aria-invalid", String(!ok));
  if (!ok) { reason.focus(); return; }
  await changeTicketStatus(ticketIdFromUrl(), "EN_PROCESO", document.getElementById("confirmReopenBtn"),
    { comment: reason.value.trim() }, "Ticket reabierto: vuelve a EN PROCESO con el técnico asignado.");
}

async function changeTicketStatus(id, status, btn, extra = {}, successMessage = null) {
  const ui = window.CampusDeskUI;
  const api = window.CampusDeskApi;
  ui.setLoading(btn, true, "Aplicando…");
  try {
    await api.changeStatus(id, status, extra);
    await renderDetail();
    ui.showAlert("actionsAlert", successMessage || `Ticket marcado como ${status.replace("_", " ")}.`, "success");
    // renderActions oculta la tarjeta si ya no hay acciones; el aviso debe verse igual.
    if (ui.el("actionsCard").style.display === "none") ui.showAlert("commentAlert",
      successMessage || `Ticket marcado como ${status.replace("_", " ")}.`, "success");
  } catch (err) {
    console.debug("[CampusDesk] Error de estado:", err);
    ui.showAlert("actionsAlert", err.message || "No se pudo cambiar el estado.", "error");
  } finally {
    ui.setLoading(btn, false);
  }
}

function renderComments(comments) {
  const ui = window.CampusDeskUI;
  const list = ui.el("commentsList");
  list.innerHTML = comments.length ? comments.map((c) => `
    <div class="comment${c.interno ? " internal" : ""}">
      <div class="comment-head"><span><b>${ui.escapeHtml(c.autor)}</b>${c.interno ? `<span class="internal-tag">Nota interna</span>` : ""}</span><span>${ui.formatDate(c.fecha)}</span></div>
      <div>${ui.escapeHtml(c.contenido)}</div>
    </div>`).join("")
    : `<div class="empty">Este ticket aún no tiene comentarios.</div>`;
}

// Flujo con los estados realmente alcanzados (solo lectura).
function renderHistory(events, currentEstado) {
  const ui = window.CampusDeskUI;
  const flow = ["ABIERTA", "ASIGNADA", "EN_PROCESO", "RESUELTA", "CERRADA"];
  const reached = new Set(events.map((e) => e.newStatus));
  reached.add(currentEstado);
  const dateOf = (s) => {
    const ev = [...events].reverse().find((e) => e.newStatus === s);
    return ev ? ui.formatDate(ev.changedAt) : null;
  };
  // Veces que el solicitante reabrió el ticket (RESUELTA → EN_PROCESO).
  const reopens = events.filter((e) => e.previousStatus === "RESUELTA" && e.newStatus === "EN_PROCESO").length;
  const reopenNote = ui.el("reopenCount");
  if (reopenNote) {
    reopenNote.style.display = reopens ? "" : "none";
    reopenNote.textContent = reopens ? `↺ Reabierto ${reopens} ${reopens === 1 ? "vez" : "veces"} por el solicitante.` : "";
  }
  ui.el("historyList").innerHTML = flow.map((s) => {
    const isCurrent = s === currentEstado;
    const wasReached = reached.has(s);
    const cls = isCurrent ? "current" : wasReached ? "done" : "";
    const date = dateOf(s);
    return `<li class="${cls}"><span class="t-dot" aria-hidden="true"></span>${ui.statusBadge(s)}
      <div class="cell-sub">${isCurrent ? "Estado actual" : wasReached && date ? date : "Flujo general"}</div></li>`;
  }).join("");
}

async function handleComment(e) {
  e.preventDefault();
  const ui = window.CampusDeskUI;
  const api = window.CampusDeskApi;
  const box = ui.el("commentText");
  if (!box.value.trim()) {
    box.setAttribute("aria-invalid", "true");
    ui.showAlert("commentAlert", "Escribe un comentario antes de enviar.", "error");
    return;
  }
  box.setAttribute("aria-invalid", "false");
  const btn = ui.el("commentBtn");
  ui.setLoading(btn, true, "Enviando…");
  try {
    const internalBox = ui.el("commentInternal");
    const internal = !!internalBox?.checked && internalBox.offsetParent !== null;
    await api.addComment(ticketIdFromUrl(), box.value.trim(), internal);
    box.value = "";
    if (internalBox) internalBox.checked = false;
    ui.hideAlert("commentAlert");
    renderComments((await api.getComments(ticketIdFromUrl())).map((c) => ui.normalizeComment(c)));
  } catch (err) {
    console.debug("[CampusDesk] Error en comentario:", err);
    ui.showAlert("commentAlert", err.message || "No se pudo enviar el comentario.", "error");
  } finally {
    ui.setLoading(btn, false);
  }
}
