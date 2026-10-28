/* CampusDesk · js/auth.js — login y registro contra la API. */
"use strict";

document.addEventListener("DOMContentLoaded", () => {
  const api = window.CampusDeskApi;
  // Con sesión guardada se valida el token contra el servidor (/me):
  // válido entra al panel, vencido o borrado vuelve al login.
  if (api?.getToken() && api?.getSessionUser()) {
    api.getMe()
      .then((user) => { api.saveSession(api.getToken(), user); location.href = "dashboard.html"; })
      .catch(() => { api.clearSession(); });
    return;
  }
  const loginForm = document.getElementById("loginForm");
  const registerForm = document.getElementById("registerForm");

  if (loginForm) loginForm.addEventListener("submit", handleLogin);
  if (registerForm) registerForm.addEventListener("submit", handleRegister);
});

function markValidity(input, valid, errorId, message) {
  const field = input.closest(".field");
  const err = document.getElementById(errorId);
  field?.classList.toggle("invalid", !valid);
  input.setAttribute("aria-invalid", String(!valid));
  if (err && message) err.textContent = message;
  return valid;
}

async function handleLogin(e) {
  e.preventDefault();
  const ui = window.CampusDeskUI;
  const api = window.CampusDeskApi;
  ui.hideAlert("loginAlert");

  const email = document.getElementById("loginEmail");
  const pass = document.getElementById("loginPassword");
  let ok = true;
  ok = markValidity(email, /\S+@\S+\.\S+/.test(email.value.trim()), "loginEmailError", "Ingresa un correo válido.") && ok;
  ok = markValidity(pass, pass.value.length > 0, "loginPasswordError", "Ingresa tu contraseña.") && ok;
  if (!ok) return;

  const btn = document.getElementById("loginBtn");
  ui.setLoading(btn, true, "Iniciando sesión…");
  try {
    const res = await api.login({ email: email.value.trim(), password: pass.value });
    api.saveSession(res.token, res.user);
    location.href = "dashboard.html";
  } catch (err) {
    console.debug("[CampusDesk] Error en login:", err);
    ui.showAlert("loginAlert", err.message || "No se pudo iniciar sesión. Inténtalo de nuevo.", "error");
  } finally {
    ui.setLoading(btn, false);
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const ui = window.CampusDeskUI;
  const api = window.CampusDeskApi;
  ui.hideAlert("registerAlert");

  const name = document.getElementById("regName");
  const email = document.getElementById("regEmail");
  const p1 = document.getElementById("regPassword");
  const p2 = document.getElementById("regPassword2");
  let ok = true;
  ok = markValidity(name, name.value.trim().length >= 2, "regNameError", "Ingresa tu nombre.") && ok;
  ok = markValidity(email, /\S+@\S+\.\S+/.test(email.value.trim()), "regEmailError", "Ingresa un correo válido.") && ok;
  ok = markValidity(p1, p1.value.length >= 8, "regPasswordError", "Mínimo 8 caracteres.") && ok;
  ok = markValidity(p2, p2.value === p1.value && p2.value.length > 0, "regPassword2Error", "Las contraseñas no coinciden.") && ok;
  // NOTA: no hay selector de rol a propósito — el backend asignará USER por defecto.
  if (!ok) return;

  const btn = document.getElementById("registerBtn");
  ui.setLoading(btn, true, "Creando cuenta…");
  try {
    await api.register({
      fullName: name.value.trim(),
      email: email.value.trim(),
      password: p1.value,
    });
    ui.showAlert("registerAlert", "Cuenta creada correctamente. Ya puedes iniciar sesión.", "success");
    e.target.reset();
  } catch (err) {
    console.debug("[CampusDesk] Error en registro:", err);
    ui.showAlert("registerAlert", err.message || "No se pudo completar el registro. Inténtalo de nuevo.", "error");
  } finally {
    ui.setLoading(btn, false);
  }
}
