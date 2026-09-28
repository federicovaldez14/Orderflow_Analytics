import { api } from '../api.js';
import { esc, numero, icono, intentar, abrirDialogo } from '../ui.js';

const TIPOS = {
  ENTRADA: ['Reposición', 'entrada', '+'],
  SALIDA_VENTA: ['Venta', 'salida', '−'],
  DEVOLUCION: ['Devolución', 'entrada', '↺'],
  MERMA: ['Merma', 'salida', '!'],
  AJUSTE: ['Ajuste por conteo', '', '='],
};

let raiz;
let ingredientes = [];
let seleccionado = null;
let firma = '';

export const inventario = {
  titulo: 'Inventario',
  async montar(contenedor) {
    raiz = contenedor;
    firma = '';
    raiz.innerHTML = `
      <header class="encabezado">
        <div><h1>Inventario</h1><p>Stock de ingredientes con trazabilidad: cada venta, reposición y ajuste queda registrado.</p></div>
      </header>
      <div class="kpis" id="kpis"></div>
      <div class="inventario">
        <section class="tarjeta">
          <div class="tabla-envoltura">
            <table>
              <thead><tr><th>Ingrediente</th><th>Stock</th><th class="num">Actual</th><th class="num">Mínimo</th><th></th></tr></thead>
              <tbody id="filas"></tbody>
            </table>
          </div>
        </section>
        <section class="tarjeta">
          <div class="tarjeta-cab"><h2 id="titulo-movs">Movimientos recientes</h2>
            <button class="btn btn-fantasma btn-vista" id="ver-todos" hidden>Ver todos</button></div>
          <ul class="feed" id="movimientos"></ul>
        </section>
      </div>`;
    raiz.querySelector('#filas').addEventListener('click', clicFila);
    raiz.querySelector('#ver-todos').addEventListener('click', () => { seleccionado = null; firma = ''; this.refrescar(); });
    await this.refrescar();
  },
  async refrescar() {
    const [ings, movs] = await Promise.all([api.inventario(), api.movimientos(seleccionado, 40)]);
    ingredientes = ings;
    const nueva = JSON.stringify([ings, movs, seleccionado]);
    if (nueva === firma) return;
    firma = nueva;
    pintarKpis();
    pintarTabla();
    pintarMovimientos(movs);
  },
};

const nombre = (codigo) => ingredientes.find((i) => i.codigo === codigo)?.nombre || codigo;

function pintarKpis() {
  const bajos = ingredientes.filter((i) => i.bajoMinimo);
  raiz.querySelector('#kpis').innerHTML = `
    <div class="kpi"><span>Ingredientes</span><b>${ingredientes.length}</b><small>con receta en la carta</small></div>
    <div class="kpi ${bajos.length ? 'alerta' : ''}"><span>Bajo el mínimo</span><b>${bajos.length}</b>
      <small>${bajos.length ? esc(bajos.map((i) => i.nombre).slice(0, 3).join(', ')) : 'todo en orden'}</small></div>`;
}

function pintarTabla() {
  raiz.querySelector('#filas').innerHTML = ingredientes.map((i) => {
    // La barra llega a 4 veces el mínimo; la marca vertical es el mínimo.
    const tope = Math.max(i.stockMinimo * 4, i.stock, 1);
    return `
      <tr class="${i.codigo === seleccionado ? 'seleccionada' : ''}" data-codigo="${esc(i.codigo)}">
        <td><b>${esc(i.nombre)}</b>
          ${i.bajoMinimo ? `<div class="alerta-stock">${icono.alerta} Bajo el mínimo</div>` : ''}</td>
        <td><div class="barra-stock ${i.bajoMinimo ? 'bajo' : ''}" title="${numero(i.stock)} de mínimo ${numero(i.stockMinimo)}">
          <i style="width:${Math.min(100, (i.stock / tope) * 100)}%"></i><em style="left:${(i.stockMinimo / tope) * 100}%"></em></div></td>
        <td class="num mono"><b>${numero(i.stock)}</b> <span class="terciario">${esc(i.unidad)}</span></td>
        <td class="num mono terciario">${numero(i.stockMinimo)}</td>
        <td><div class="acciones-fila">
          <button class="btn" data-reponer="${esc(i.codigo)}">Reponer</button>
          <button class="btn btn-fantasma" data-ajustar="${esc(i.codigo)}">Ajustar</button>
        </div></td>
      </tr>`;
  }).join('');
}

function pintarMovimientos(movs) {
  raiz.querySelector('#titulo-movs').textContent = seleccionado ? `Movimientos · ${nombre(seleccionado)}` : 'Movimientos recientes';
  raiz.querySelector('#ver-todos').hidden = !seleccionado;
  raiz.querySelector('#movimientos').innerHTML = movs.length ? movs.map((m) => {
    const [texto, clase, simbolo] = TIPOS[m.tipo] || [m.tipo, '', '·'];
    const hora = new Date(m.fecha).toLocaleString('es-CO', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
    return `
      <li>
        <span class="icono ${clase}" aria-hidden="true">${simbolo}</span>
        <span class="texto"><b>${esc(nombre(m.ingrediente))} · ${esc(texto)}</b><span>${esc(m.descripcion || '')} · ${hora}</span></span>
        <span class="cant mono">${numero(m.cantidad)}<small>queda ${numero(m.stockResultante)}</small></span>
      </li>`;
  }).join('') : `<li class="vacio" style="display:block">Sin movimientos todavía</li>`;
}

async function clicFila(ev) {
  const b = ev.target.closest('button');
  if (b?.dataset.reponer) return dialogoCantidad(b.dataset.reponer, 'reponer');
  if (b?.dataset.ajustar) return dialogoCantidad(b.dataset.ajustar, 'ajustar');
  const fila = ev.target.closest('tr[data-codigo]');
  if (fila) {
    seleccionado = seleccionado === fila.dataset.codigo ? null : fila.dataset.codigo;
    firma = '';
    await inventario.refrescar();
  }
  return undefined;
}

function dialogoCantidad(codigo, modo) {
  const ing = ingredientes.find((i) => i.codigo === codigo);
  const reponer = modo === 'reponer';
  const d = abrirDialogo(`
    <form method="dialog">
      <div class="dialogo-cab"><div>
        <h2>${reponer ? 'Reponer' : 'Ajustar'} ${esc(ing.nombre)}</h2>
        <p>${reponer ? 'Registra una compra o entrada de mercancía.' : 'Corrige el stock con lo que se contó físicamente.'}
           Stock actual: <b>${numero(ing.stock)} ${esc(ing.unidad)}</b></p>
      </div></div>
      <div class="dialogo-cuerpo">
        <div class="campo"><label for="cant">${reponer ? 'Cantidad que entra' : 'Stock contado'} (${esc(ing.unidad)})</label>
          <input class="entrada mono" id="cant" type="number" min="${reponer ? 1 : 0}" step="1" required autofocus></div>
        <div class="campo"><label for="nota">Nota (opcional)</label>
          <input class="entrada" id="nota" maxlength="120" placeholder="${reponer ? 'Proveedor, factura…' : 'Conteo de cierre'}"></div>
      </div>
      <div class="dialogo-pie">
        <button class="btn btn-fantasma" type="button" data-cerrar>Cancelar</button>
        <button class="btn btn-primario" type="submit">${reponer ? 'Registrar entrada' : 'Guardar conteo'}</button>
      </div>
    </form>`, 'chico');
  d.querySelector('form').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const cantidad = Number(d.querySelector('#cant').value);
    const nota = d.querySelector('#nota').value.trim() || (reponer ? 'Reposición' : 'Conteo físico');
    const r = await intentar(() => (reponer ? api.reponer(codigo, cantidad, nota) : api.ajustar(codigo, cantidad, nota)),
      `${ing.nombre}: stock actualizado`);
    if (r) {
      d.close();
      firma = '';
      await inventario.refrescar();
    }
  });
}
