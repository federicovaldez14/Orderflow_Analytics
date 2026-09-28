// Utilidades de presentación compartidas por todas las vistas.

const formatoPesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const formatoNumero = new Intl.NumberFormat('es-CO');

export const pesos = (v) => formatoPesos.format(v || 0);
export const numero = (v) => formatoNumero.format(v || 0);

/** Escapa texto que viene de la API antes de meterlo en HTML. */
export function esc(texto) {
  return String(texto ?? '').replace(/[&<>"']/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** Clase CSS y siguiente acción para cada estado del pedido (State del dominio). */
export const ESTADOS = {
  'Libre': { clase: 'estado-libre', siguiente: null },
  'Creado': { clase: 'estado-creado', siguiente: 'Iniciar preparación' },
  'En preparación': { clase: 'estado-preparacion', siguiente: 'Marcar como listo' },
  'Listo': { clase: 'estado-listo', siguiente: 'Entregar a la mesa' },
  'Entregado': { clase: 'estado-cerrado', siguiente: null },
  'Cancelado': { clase: 'estado-cerrado', siguiente: null },
};
export const FLUJO = ['Creado', 'En preparación', 'Listo', 'Entregado'];
export const claseEstado = (estado) => (ESTADOS[estado] || ESTADOS.Libre).clase;
export const pastilla = (estado) => `<span class="pastilla ${claseEstado(estado)}">${esc(estado)}</span>`;
export const activo = (p) => p.estado !== 'Entregado' && p.estado !== 'Cancelado';

/** "hace 12 min" a partir de una fecha ISO local del servidor. */
export function transcurrido(iso) {
  if (!iso) return '';
  const min = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
  if (min < 1) return 'ahora';
  if (min < 60) return `${min} min`;
  const h = Math.floor(min / 60);
  return `${h} h ${min % 60} min`;
}
export const minutosDesde = (iso) => (iso ? (Date.now() - new Date(iso).getTime()) / 60000 : 0);

export const icono = {
  ok: '<svg viewBox="0 0 24 24"><path d="M20 6 9 17l-5-5"/></svg>',
  error: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 8v5M12 16.5v.01"/></svg>',
  alerta: '<svg viewBox="0 0 24 24"><path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"/><path d="M12 9v4M12 17v.01"/></svg>',
  mas: '<svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>',
  x: '<svg viewBox="0 0 24 24"><path d="M18 6 6 18M6 6l12 12"/></svg>',
  flecha: '<svg viewBox="0 0 24 24"><path d="M5 12h14M13 6l6 6-6 6"/></svg>',
  dividir: '<svg viewBox="0 0 24 24"><path d="M16 3h5v5M8 3H3v5M21 3l-7 7M3 3l7 7M12 12v9"/></svg>',
  vacio: '<svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="16" rx="3"/><path d="M3 9h18"/></svg>',
};

/** Aviso flotante. Para errores de stock muestra la lista de faltantes. */
export function avisar(titulo, detalle = '', tipo = 'ok', faltantes = []) {
  const caja = document.getElementById('toasts');
  const el = document.createElement('div');
  el.className = `toast ${tipo}`;
  const lista = faltantes.length
    ? `<ul>${faltantes.map((f) => `<li>${esc(f.nombre || f.codigo)}: necesita ${numero(f.requerido)}, hay ${numero(f.disponible)}</li>`).join('')}</ul>`
    : '';
  el.innerHTML = `${tipo === 'ok' ? icono.ok : icono.error}<div><b>${esc(titulo)}</b>${detalle ? `<span>${esc(detalle)}</span>` : ''}${lista}</div>`;
  caja.appendChild(el);
  setTimeout(() => el.remove(), tipo === 'ok' ? 3200 : 6500);
}

/** Ejecuta una acción contra la API y muestra el resultado; el dominio decide, la web solo informa. */
export async function intentar(accion, exito) {
  try {
    const r = await accion();
    if (exito) avisar(exito);
    return r;
  } catch (e) {
    avisar('No se pudo completar', e.message, 'error', e.faltantes || []);
    return undefined;
  }
}

/** Abre un <dialog> modal con el HTML dado y devuelve el elemento. Se elimina al cerrarse. */
export function abrirDialogo(html, clase = '') {
  const d = document.createElement('dialog');
  d.className = clase;
  d.innerHTML = html;
  document.body.appendChild(d);
  d.addEventListener('close', () => d.remove());
  d.addEventListener('click', (ev) => {
    if (ev.target === d || ev.target.closest('[data-cerrar]')) d.close();
  });
  d.showModal();
  return d;
}

/** Pide confirmación con un diálogo propio (no el confirm() del navegador). */
export function confirmar(titulo, texto, textoBoton, peligro = false) {
  return new Promise((resolver) => {
    const d = abrirDialogo(`
      <div class="dialogo-cab"><div><h2>${esc(titulo)}</h2><p>${esc(texto)}</p></div></div>
      <div class="dialogo-pie">
        <button class="btn btn-fantasma" data-cerrar>Volver</button>
        <button class="btn ${peligro ? 'btn-peligro' : 'btn-primario'}" data-si>${esc(textoBoton)}</button>
      </div>`, 'chico');
    let respuesta = false;
    d.querySelector('[data-si]').addEventListener('click', () => { respuesta = true; d.close(); });
    d.addEventListener('close', () => resolver(respuesta));
  });
}

/** Tooltip compartido para las gráficas. */
const tip = () => document.getElementById('tooltip');
export function mostrarTooltip(x, y, html) {
  const t = tip();
  t.innerHTML = html;
  t.hidden = false;
  const ancho = t.offsetWidth / 2 + 8;
  t.style.left = `${Math.min(Math.max(x, ancho), window.innerWidth - ancho)}px`;
  t.style.top = `${y}px`;
}
export const ocultarTooltip = () => { tip().hidden = true; };
