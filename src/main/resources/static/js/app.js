// Punto de entrada de la web: enrutador por hash, refresco periódico de la
// vista visible (igual que el POS Swing cada 2 s) y estado de conexión.
import { api } from './api.js';
import { activo } from './ui.js';
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

function marcarConexion(ok) {
  const el = document.getElementById('conexion');
  el.classList.toggle('ok', ok);
  el.classList.toggle('error', !ok);
  el.querySelector('span').textContent = ok ? 'En línea' : 'Sin conexión con el servidor';
}

async function refrescar() {
  if (!actual || document.hidden) return;
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
  const nombre = (location.hash.replace('#/', '') || 'salon').split('?')[0];
  const vista = VISTAS[nombre] || salon;
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
    contenido.innerHTML = `<div class="vacio"><b>No se pudo cargar ${vista.titulo.toLowerCase()}</b><p>${e.message}</p>
      <p>Verifica que la aplicación esté corriendo (<code>mvn spring-boot:run</code>).</p></div>`;
  }
  contenido.focus({ preventScroll: true });
}

function reloj() {
  document.getElementById('reloj').textContent =
    new Date().toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', hour12: false });
}

window.addEventListener('hashchange', navegar);
document.addEventListener('visibilitychange', refrescar);
reloj();
setInterval(reloj, 10000);
clearInterval(temporizador);
temporizador = setInterval(refrescar, 500);
navegar();
