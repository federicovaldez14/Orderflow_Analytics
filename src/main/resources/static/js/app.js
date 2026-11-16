// Punto de entrada de la web: sesión y roles, enrutador por hash, refresco
// periódico de la vista visible (igual que el POS Swing cada 2 s) y estado de conexión.
import { api } from './api.js';
import { activo, esc } from './ui.js';
import { salon } from './vistas/salon.js';
import { cocina } from './vistas/cocina.js';
import { inventario } from './vistas/inventario.js';
import { analitica } from './vistas/analitica.js';
import { actividad } from './vistas/actividad.js';

const VISTAS = { salon, cocina, inventario, analitica, actividad };
const INTERVALO = 2500;

const contenido = document.getElementById('contenido');
let actual = null;
let temporizador = null;
let ultimoRefresco = 0;
let sesion = null;   // { usuario, roles } o null sin sesión

const NOMBRE_ROL = { ADMIN: 'Administrador', MESERO: 'Mesero', COCINA: 'Cocina', MONITOR: 'Monitoreo' };

/** Vistas que permite el rol (data-roles en cada enlace del menú). */
function vistasPermitidas() {
  return [...document.querySelectorAll('#nav a')]
    .filter((a) => sesion && a.dataset.roles.split(' ').some((r) => sesion.roles.includes(r)))
    .map((a) => a.dataset.vista);
}

function aplicarSesion() {
  const permitidas = vistasPermitidas();
  document.querySelectorAll('#nav a').forEach((a) => a.classList.toggle('oculto-por-rol', !permitidas.includes(a.dataset.vista)));
  document.getElementById('usuario-actual').hidden = !sesion;
  if (sesion) {
    document.getElementById('usuario-nombre').textContent = sesion.usuario;
    document.getElementById('usuario-rol').textContent = sesion.roles.map((r) => NOMBRE_ROL[r] || r).join(', ');
  }
}

function mostrarLogin(mensaje = '') {
  sesion = null;
  actual = null;
  aplicarSesion();
  contenido.innerHTML = '';
  const error = document.getElementById('login-error');
  error.textContent = mensaje;
  error.hidden = !mensaje;
  document.getElementById('pantalla-login').hidden = false;
  document.getElementById('login-usuario').focus();
}

async function entrar(ev) {
  ev.preventDefault();
  const boton = document.getElementById('login-entrar');
  boton.disabled = true;
  try {
    sesion = await api.iniciarSesion(document.getElementById('login-usuario').value.trim(),
      document.getElementById('login-clave').value);
    document.getElementById('login-clave').value = '';
    document.getElementById('pantalla-login').hidden = true;
    aplicarSesion();
    navegar();
  } catch (e) {
    mostrarLogin(e.status === 401 ? 'Usuario o clave incorrectos.' : 'No se pudo conectar con el servidor.');
  } finally {
    boton.disabled = false;
  }
}

async function salir() {
  try { await api.cerrarSesion(); } catch { /* la sesión ya no existe */ }
  location.hash = '';
  mostrarLogin();
}

function marcarConexion(ok) {
  const el = document.getElementById('conexion');
  el.classList.toggle('ok', ok);
  el.classList.toggle('error', !ok);
  el.querySelector('span').textContent = ok ? 'En línea' : 'Sin conexión con el servidor';
}

async function refrescar() {
  if (!actual || !sesion || document.hidden) return;
  const espera = actual.intervalo || INTERVALO;
  if (Date.now() - ultimoRefresco < espera - 100) return;
  ultimoRefresco = Date.now();
  try {
    await Promise.all([actual.refrescar(), insignias()]);
    marcarConexion(true);
  } catch {
    marcarConexion(false);
  }
}

async function insignias() {
  const [pedidos, bajos] = await Promise.all([api.pedidos(), api.inventario()]);
  const porPreparar = pedidos.filter((p) => activo(p) && p.estado !== 'Listo').length;
  const alertas = bajos.filter((i) => i.bajoMinimo).length;
  const ic = document.getElementById('insignia-cocina');
  ic.hidden = !porPreparar;
  ic.textContent = porPreparar;
  const ii = document.getElementById('insignia-inventario');
  ii.hidden = !alertas;
  ii.textContent = alertas;
}

async function navegar() {
  if (!sesion) return;
  const permitidas = vistasPermitidas();
  let nombre = (location.hash.replace('#/', '') || permitidas[0]).split('?')[0];
  if (!permitidas.includes(nombre)) {
    nombre = permitidas[0];
    history.replaceState(null, '', `#/${nombre}`);
  }
  const vista = VISTAS[nombre];
  if (actual?.desmontar) actual.desmontar();
  actual = vista;
  document.querySelectorAll('#nav a').forEach((a) => {
    const es = a.dataset.vista === nombre;
    a.classList.toggle('activo', es);
    if (es) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
  });
  document.title = `${vista.titulo} · Orderflow POS`;
  contenido.innerHTML = '<div class="esqueleto" style="height:420px"></div>';
  try {
    await vista.montar(contenido);
    ultimoRefresco = Date.now();
    await insignias();
    marcarConexion(true);
  } catch (e) {
    marcarConexion(false);
    if (e.status === 401) return;   // sesión vencida: el evento sesion-expirada ya mostró el login
    contenido.innerHTML = `<div class="vacio"><b>No se pudo cargar ${esc(vista.titulo.toLowerCase())}</b><p>${esc(e.message)}</p>
      <p>Verifica que la aplicación esté corriendo.</p></div>`;
  }
  contenido.focus({ preventScroll: true });
}

function reloj() {
  document.getElementById('reloj').textContent =
    new Date().toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', hour12: false });
}

async function iniciar() {
  try {
    sesion = await api.sesion();
    document.getElementById('pantalla-login').hidden = true;
    aplicarSesion();
    navegar();
  } catch {
    mostrarLogin();
  }
}

window.addEventListener('hashchange', navegar);
window.addEventListener('sesion-expirada', () => { if (sesion) mostrarLogin('Su sesión terminó. Vuelva a entrar.'); });
document.addEventListener('visibilitychange', refrescar);
document.getElementById('form-login').addEventListener('submit', entrar);
document.getElementById('cerrar-sesion').addEventListener('click', salir);
reloj();
setInterval(reloj, 10000);
clearInterval(temporizador);
temporizador = setInterval(refrescar, 500);
iniciar();
