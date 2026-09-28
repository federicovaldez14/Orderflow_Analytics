import { api } from '../api.js';
import { esc, pesos, numero, mostrarTooltip, ocultarTooltip } from '../ui.js';

// Cada reporte del dominio es una sola serie (etiqueta -> valor): un solo
// color, sin leyenda; el título la nombra. Las horas se dibujan en columnas
// (orden temporal) y los rankings en barras horizontales.
const PERIODOS = [['HOY', 'Hoy'], ['SEMANA', 'Semana'], ['TODO', 'Todo']];
const compacto = new Intl.NumberFormat('es-CO', { notation: 'compact', maximumFractionDigits: 1 });

let raiz;
let periodo = 'SEMANA';
let panel = null;
const comoTabla = new Set();
let firma = '';

export const analitica = {
  titulo: 'Analítica',
  intervalo: 10000,
  async montar(contenedor) {
    raiz = contenedor;
    firma = '';
    raiz.innerHTML = `
      <header class="encabezado">
        <div><h1>Analítica</h1><p id="considerados">Ventas, platos y tiempos de atención.</p></div>
        <div class="segmentado" role="group" aria-label="Periodo" id="periodos">
          ${PERIODOS.map(([p, t]) => `<button data-periodo="${p}" aria-pressed="${p === periodo}">${t}</button>`).join('')}
        </div>
      </header>
      <div class="kpis" id="kpis">${'<div class="kpi esqueleto" style="height:92px"></div>'.repeat(6)}</div>
      <div class="graficas" id="graficas"></div>`;
    raiz.querySelector('#periodos').addEventListener('click', (ev) => {
      const b = ev.target.closest('[data-periodo]');
      if (!b) return;
      periodo = b.dataset.periodo;
      raiz.querySelectorAll('#periodos button').forEach((x) => x.setAttribute('aria-pressed', x === b));
      firma = '';
      this.refrescar();
    });
    raiz.querySelector('#graficas').addEventListener('click', (ev) => {
      const b = ev.target.closest('[data-vista]');
      if (!b) return;
      const id = b.dataset.vista;
      if (comoTabla.has(id)) comoTabla.delete(id); else comoTabla.add(id);
      pintarGraficas();
    });
    window.addEventListener('resize', alRedimensionar);
    await this.refrescar();
  },
  desmontar() {
    window.removeEventListener('resize', alRedimensionar);
    ocultarTooltip();
  },
  async refrescar() {
    const datos = await api.analitica(periodo);
    const nueva = JSON.stringify(datos);
    if (nueva === firma) return;
    firma = nueva;
    panel = datos;
    pintarKpis();
    pintarGraficas();
  },
};

let temporizador;
function alRedimensionar() {
  clearTimeout(temporizador);
  temporizador = setTimeout(pintarGraficas, 120);
}

const formatear = (v, unidad) => (unidad === '$' ? pesos(v) : `${numero(Math.round(v * 10) / 10)} ${unidad}`);
const formatearEje = (v, unidad) => (unidad === '$' ? `$${compacto.format(v)}` : compacto.format(v));

function pintarKpis() {
  const k = panel.indicadores;
  raiz.querySelector('#considerados').textContent =
    `${numero(panel.pedidosConsiderados)} pedidos en el periodo · ventas, platos y tiempos de atención.`;
  raiz.querySelector('#kpis').innerHTML = `
    <div class="kpi destacado"><span>Ventas</span><b class="mono">${pesos(k.ventas)}</b><small>pedidos entregados</small></div>
    <div class="kpi"><span>Ticket promedio</span><b class="mono">${pesos(k.ticketPromedio)}</b><small>por pedido entregado</small></div>
    <div class="kpi"><span>Entregados</span><b class="mono">${numero(k.pedidosEntregados)}</b><small>${numero(k.pedidosEnCurso)} en curso</small></div>
    <div class="kpi"><span>Tiempo de atención</span><b class="mono">${numero(Math.round(k.tiempoPromedioMin * 10) / 10)} min</b><small>de la comanda a la mesa</small></div>
    <div class="kpi"><span>Cancelación</span><b class="mono">${numero(Math.round(k.tasaCancelacion * 1000) / 10)} %</b><small>${numero(k.pedidosCancelados)} pedidos</small></div>`;
}

function pintarGraficas() {
  if (!panel) return;
  const cont = raiz.querySelector('#graficas');
  cont.innerHTML = panel.reportes.map((r) => `
    <article class="tarjeta grafica ${r.id === 'ventas-por-hora' ? 'ancha' : ''}" data-reporte="${esc(r.id)}">
      <div class="tarjeta-cab">
        <div><h3>${esc(r.titulo)}</h3><p>${r.unidad === '$' ? 'Pesos colombianos' : `En ${esc(r.unidad)}`}</p></div>
        <button class="btn btn-fantasma btn-vista" data-vista="${esc(r.id)}">${comoTabla.has(r.id) ? 'Ver gráfica' : 'Ver tabla'}</button>
      </div>
      <div class="tarjeta-cuerpo"></div>
    </article>`).join('');
  panel.reportes.forEach((r) => {
    const cuerpo = cont.querySelector(`[data-reporte="${CSS.escape(r.id)}"] .tarjeta-cuerpo`);
    if (!r.datos.length || r.datos.every((d) => d.valor === 0)) {
      cuerpo.innerHTML = '<div class="vacio">Sin datos en este periodo</div>';
    } else if (comoTabla.has(r.id)) {
      cuerpo.innerHTML = tabla(r);
    } else if (r.id === 'ventas-por-hora') {
      columnas(cuerpo, r);
    } else {
      barras(cuerpo, r);
    }
  });
}

function tabla(r) {
  return `<table><thead><tr><th>${r.id === 'ventas-por-hora' ? 'Hora' : 'Elemento'}</th><th class="num">Valor</th></tr></thead>
    <tbody>${r.datos.map((d) => `<tr><td>${esc(d.etiqueta)}</td><td class="num mono">${formatear(d.valor, r.unidad)}</td></tr>`).join('')}</tbody></table>`;
}

/** Marcas rectangulares con esquinas de 4px solo en el extremo del dato. */
function rectRedondeado(x, y, w, h, horizontal) {
  const r = Math.min(4, horizontal ? w : h, horizontal ? h / 2 : w / 2);
  if (w <= 0 || h <= 0) return '';
  return horizontal
    ? `M${x},${y}h${w - r}a${r},${r} 0 0 1 ${r},${r}v${h - 2 * r}a${r},${r} 0 0 1 -${r},${r}h-${w - r}z`
    : `M${x},${y + h}v-${h - r}a${r},${r} 0 0 1 ${r},-${r}h${w - 2 * r}a${r},${r} 0 0 1 ${r},${r}v${h - r}z`;
}

function ticks(max) {
  const paso = 10 ** Math.floor(Math.log10(max / 4 || 1));
  const mult = [1, 2, 2.5, 5, 10].find((m) => max / (paso * m) <= 5) || 10;
  const salto = paso * mult;
  const lista = [];
  for (let v = 0; v <= max + salto * 0.001; v += salto) lista.push(v);
  if (lista[lista.length - 1] < max) lista.push(lista[lista.length - 1] + salto);
  return lista;
}

function conectarTooltip(svg, r) {
  svg.addEventListener('mousemove', (ev) => {
    const g = ev.target.closest('g[data-i]');
    svg.querySelectorAll('g[data-i]').forEach((x) => x.classList.toggle('atenuada', g && x !== g));
    if (!g) return ocultarTooltip();
    const d = r.datos[Number(g.dataset.i)];
    const caja = g.querySelector('.barra').getBoundingClientRect();
    return mostrarTooltip(caja.left + caja.width / 2, caja.top, `${esc(d.etiqueta)}<b>${formatear(d.valor, r.unidad)}</b>`);
  });
  svg.addEventListener('mouseleave', () => {
    ocultarTooltip();
    svg.querySelectorAll('g[data-i]').forEach((x) => x.classList.remove('atenuada'));
  });
}

function columnas(cuerpo, r) {
  const ancho = Math.max(cuerpo.clientWidth, 280);
  const alto = 240;
  const m = { arriba: 22, derecha: 8, abajo: 28, izquierda: 56 };
  const max = Math.max(...r.datos.map((d) => d.valor));
  const escala = ticks(max);
  const tope = escala[escala.length - 1];
  const ai = ancho - m.izquierda - m.derecha;
  const hi = alto - m.arriba - m.abajo;
  const banda = ai / r.datos.length;
  const bw = Math.min(40, banda * 0.62);
  const y = (v) => m.arriba + hi - (v / tope) * hi;
  const iMax = r.datos.findIndex((d) => d.valor === max);
  const cadaCuanto = banda < 34 ? 2 : 1;

  cuerpo.innerHTML = `
    <svg width="${ancho}" height="${alto}" role="img" aria-label="${esc(r.titulo)}">
      ${escala.map((t) => `<line class="${t === 0 ? 'eje' : 'rejilla'}" x1="${m.izquierda}" x2="${ancho - m.derecha}" y1="${y(t)}" y2="${y(t)}"/>
        <text x="${m.izquierda - 8}" y="${y(t) + 4}" text-anchor="end">${formatearEje(t, r.unidad)}</text>`).join('')}
      ${r.datos.map((d, i) => {
        const cx = m.izquierda + banda * i + banda / 2;
        return `<g data-i="${i}">
          <rect class="objetivo" x="${cx - banda / 2}" y="${m.arriba}" width="${banda}" height="${hi}"/>
          <path class="barra" d="${rectRedondeado(cx - bw / 2, y(d.valor), bw, y(0) - y(d.valor), false)}"/>
          ${i % cadaCuanto === 0 ? `<text x="${cx}" y="${alto - 8}" text-anchor="middle">${esc(d.etiqueta)}</text>` : ''}
          ${i === iMax ? `<text class="etiqueta-valor" x="${cx}" y="${y(d.valor) - 7}" text-anchor="middle">${formatearEje(d.valor, r.unidad)}</text>` : ''}
        </g>`;
      }).join('')}
    </svg>`;
  conectarTooltip(cuerpo.querySelector('svg'), r);
}

function barras(cuerpo, r) {
  const ancho = Math.max(cuerpo.clientWidth, 280);
  const fila = 28;
  const m = { arriba: 4, derecha: 64, abajo: 22, izquierda: Math.min(170, ancho * 0.42) };
  const alto = m.arriba + m.abajo + fila * r.datos.length;
  const max = Math.max(...r.datos.map((d) => d.valor));
  const ai = ancho - m.izquierda - m.derecha;
  const x = (v) => m.izquierda + (v / (max || 1)) * ai;
  const bh = 14;

  cuerpo.innerHTML = `
    <svg width="${ancho}" height="${alto}" role="img" aria-label="${esc(r.titulo)}">
      <line class="eje" x1="${m.izquierda}" x2="${m.izquierda}" y1="${m.arriba}" y2="${alto - m.abajo}"/>
      ${r.datos.map((d, i) => {
        const cy = m.arriba + fila * i + fila / 2;
        const etiqueta = d.etiqueta.length > 24 ? `${d.etiqueta.slice(0, 23)}…` : d.etiqueta;
        return `<g data-i="${i}">
          <rect class="objetivo" x="0" y="${cy - fila / 2}" width="${ancho}" height="${fila}"/>
          <text x="${m.izquierda - 10}" y="${cy + 4}" text-anchor="end">${esc(etiqueta)}</text>
          <path class="barra" d="${rectRedondeado(m.izquierda, cy - bh / 2, x(d.valor) - m.izquierda, bh, true)}"/>
          <text class="${d.valor === max ? 'etiqueta-valor' : ''}" x="${x(d.valor) + 8}" y="${cy + 4}">${formatearEje(d.valor, r.unidad)}</text>
        </g>`;
      }).join('')}
    </svg>`;
  conectarTooltip(cuerpo.querySelector('svg'), r);
}
