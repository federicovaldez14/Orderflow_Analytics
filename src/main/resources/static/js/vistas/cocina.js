import { api } from '../api.js';
import { esc, activo, transcurrido, minutosDesde, ESTADOS, icono, intentar, avisar, confirmar } from '../ui.js';

const COLUMNAS = [
  ['Creado', 'Por preparar', 'creado'],
  ['En preparación', 'En preparación', 'preparacion'],
  ['Listo', 'Listos para entregar', 'listo'],
];
const MINUTOS_TARDE = 20;

let raiz;
let firma = '';

export const cocina = {
  titulo: 'Cocina',
  async montar(contenedor) {
    raiz = contenedor;
    firma = '';
    raiz.innerHTML = `
      <header class="encabezado">
        <div><h1>Cocina</h1><p>Comandas activas por estado. Las que llevan más de ${MINUTOS_TARDE} min se marcan en ámbar.</p></div>
      </header>
      <div class="tablero" id="tablero"></div>`;
    raiz.querySelector('#tablero').addEventListener('click', clic);
    await this.refrescar();
  },
  async refrescar() {
    const pedidos = (await api.pedidos()).filter(activo).sort((a, b) => a.horaCreacion.localeCompare(b.horaCreacion));
    const nueva = JSON.stringify([pedidos, Math.floor(Date.now() / 60000)]);
    if (nueva === firma) return;
    firma = nueva;
    raiz.querySelector('#tablero').innerHTML = COLUMNAS.map(([estado, titulo, v]) => {
      const lista = pedidos.filter((p) => p.estado === estado);
      return `
        <section class="columna" style="--c: var(--${v})" aria-label="${esc(titulo)}">
          <div class="columna-cab"><h2><i></i>${esc(titulo)}</h2><span>${lista.length}</span></div>
          <div class="comandas">
            ${lista.length ? lista.map((p) => comanda(p, v)).join('') : `<div class="vacio">${icono.vacio}Sin comandas</div>`}
          </div>
        </section>`;
    }).join('');
  },
};

function comanda(p, v) {
  const tarde = minutosDesde(p.horaCreacion) > MINUTOS_TARDE;
  return `
    <article class="comanda" style="--c: var(--${v})">
      <div class="comanda-cab">
        <b>Mesa ${p.mesa}</b>
        <span class="${tarde ? 'tarde' : ''}">#${p.id} · ${transcurrido(p.horaCreacion)}</span>
      </div>
      <ul>${p.lineas.map((l) => `<li><b>${l.cantidad}×</b>${esc(l.plato)}</li>`).join('')}</ul>
      <div class="comanda-pie">
        <button class="btn btn-primario" data-avanzar="${p.id}">${esc(ESTADOS[p.estado].siguiente)}</button>
        <button class="btn btn-fantasma btn-icono" data-cancelar="${p.id}" aria-label="Cancelar pedido ${p.id}">${icono.x}</button>
      </div>
    </article>`;
}

async function clic(ev) {
  const b = ev.target.closest('button');
  if (!b) return;
  if (b.dataset.avanzar) {
    b.disabled = true;
    const p = await intentar(() => api.avanzar(Number(b.dataset.avanzar)));
    if (p) avisar(`Mesa ${p.mesa}: ${p.estado}`);
  } else if (b.dataset.cancelar) {
    const id = Number(b.dataset.cancelar);
    if (!await confirmar(`¿Cancelar el pedido #${id}?`, 'Se avisará al mesero y el inventario no usado se devuelve.', 'Cancelar pedido', true)) return;
    await intentar(() => api.cancelar(id), `Pedido #${id} cancelado`);
  }
  await cocina.refrescar();
}
