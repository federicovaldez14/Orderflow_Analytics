import { api } from '../api.js';
import {
  esc, pesos, pastilla, claseEstado, ESTADOS, FLUJO, activo, transcurrido, icono, intentar, confirmar, avisar,
} from '../ui.js';
import { abrirDivision } from '../division.js';

const CATEGORIAS = [
  ['TODO', 'Todo'], ['ENTRADA', 'Entradas'], ['FUERTE', 'Fuertes'], ['BEBIDA', 'Bebidas'], ['POSTRE', 'Postres'],
];

const estado = {
  mesas: [], pedidos: new Map(), menu: [], disponibilidad: {},
  mesaSel: null, ticket: new Map(), categoria: 'TODO', agregando: false,
  consumo: { id: null, lista: [] }, nombresIngredientes: {},
  firmaMesas: '', firmaPanel: '',
};
let raiz;

export const salon = {
  titulo: 'Salón',
  async montar(contenedor) {
    raiz = contenedor;
    raiz.innerHTML = `
      <header class="encabezado">
        <div><h1>Salón</h1><p>Toca una mesa para tomar la comanda o ver su pedido.</p></div>
      </header>
      <div class="resumen" id="resumen"></div>
      <div class="salon">
        <section class="mesas" id="mesas" aria-label="Mesas"></section>
        <aside class="tarjeta panel-mesa" id="panel-mesa" aria-live="polite"></aside>
      </div>`;
    raiz.querySelector('#mesas').addEventListener('click', (ev) => {
      const b = ev.target.closest('[data-mesa]');
      if (b) seleccionar(Number(b.dataset.mesa));
    });
    const panel = raiz.querySelector('#panel-mesa');
    panel.addEventListener('click', clicPanel);
    estado.firmaMesas = estado.firmaPanel = '';
    const [menu, ings] = await Promise.all([api.menu(), api.inventario(), this.refrescar()]);
    estado.menu = menu;
    estado.nombresIngredientes = Object.fromEntries(ings.map((i) => [i.codigo, i.nombre]));
    estado.firmaPanel = '';
    pintarPanel();
  },
  async refrescar() {
    const [mesas, pedidos, disp] = await Promise.all([api.mesas(), api.pedidos(), api.disponibilidad()]);
    estado.mesas = mesas;
    estado.pedidos = new Map(pedidos.filter(activo).map((p) => [p.id, p]));
    estado.disponibilidad = disp;
    pintarMesas();
    pintarPanel();
  },
};

const pedidoDeMesa = (m) => (m && m.pedidoId ? estado.pedidos.get(m.pedidoId) : null);
const mesaSel = () => estado.mesas.find((m) => m.mesa === estado.mesaSel);

function seleccionar(n) {
  if (estado.mesaSel !== n) {
    estado.mesaSel = n;
    estado.ticket = new Map();
    estado.agregando = false;
  }
  pintarMesas(true);
  pintarPanel(true);
  if (window.matchMedia('(max-width: 1080px)').matches) {
    raiz.querySelector('#panel-mesa').scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

// ---------- Mapa de mesas ----------

function pintarMesas(forzar = false) {
  const firma = JSON.stringify([estado.mesas, estado.mesaSel, [...estado.pedidos.keys()], Math.floor(Date.now() / 60000)]);
  if (!forzar && firma === estado.firmaMesas) return;
  estado.firmaMesas = firma;

  const cuenta = { Libre: 0, Creado: 0, 'En preparación': 0, Listo: 0 };
  let abierto = 0;
  for (const m of estado.mesas) {
    cuenta[m.libre ? 'Libre' : m.estado] = (cuenta[m.libre ? 'Libre' : m.estado] || 0) + 1;
    abierto += m.total;
  }
  raiz.querySelector('#resumen').innerHTML = Object.entries(cuenta).map(([e, n]) => `
      <div class="chip ${claseEstado(e)}" style="--c: var(--${varEstado(e)})"><i></i><b>${n}</b><span>${e === 'Libre' ? 'Libres' : esc(e)}</span></div>`).join('')
    + `<div class="chip total"><span>Cuentas abiertas</span><b>${pesos(abierto)}</b></div>`;

  raiz.querySelector('#mesas').innerHTML = estado.mesas.map((m) => {
    const p = pedidoDeMesa(m);
    return `
      <button class="mesa ${m.libre ? 'libre' : ''}" data-mesa="${m.mesa}" aria-pressed="${m.mesa === estado.mesaSel}"
              style="--c: var(--${varEstado(m.estado)})" aria-label="Mesa ${m.mesa}, ${esc(m.estado)}">
        <div class="mesa-num"><small>MESA</small>${String(m.mesa).padStart(2, '0')}</div>
        ${p ? `<span class="mesa-tiempo">${transcurrido(p.horaCreacion)}</span>` : ''}
        <div class="mesa-pie">
          ${pastilla(m.estado)}
          ${m.libre ? '' : `<span class="mesa-total mono">${pesos(m.total)}</span>`}
        </div>
      </button>`;
  }).join('');
}

function varEstado(e) {
  return { Creado: 'creado', 'En preparación': 'preparacion', Listo: 'listo' }[e] || 'libre';
}

// ---------- Panel lateral ----------

function pintarPanel(forzar = false) {
  const panel = raiz.querySelector('#panel-mesa');
  const m = mesaSel();
  const p = pedidoDeMesa(m);
  const firma = JSON.stringify([estado.mesaSel, m, p, [...estado.ticket], estado.categoria, estado.agregando,
    estado.disponibilidad, estado.menu.length, estado.consumo]);
  if (!forzar && firma === estado.firmaPanel) return;
  estado.firmaPanel = firma;

  const scroll = panel.querySelector('.panel-desplazable')?.scrollTop || 0;
  if (!m) {
    panel.innerHTML = `<div class="vacio">${icono.vacio}<b>Selecciona una mesa</b><p>Las mesas libres abren una comanda nueva; las ocupadas muestran su pedido.</p></div>`;
    return;
  }
  panel.innerHTML = p ? htmlPedido(m, p) : htmlNueva(m);
  const cuerpo = panel.querySelector('.panel-desplazable');
  if (cuerpo) cuerpo.scrollTop = scroll;
  if (p && estado.consumo.id !== p.id) cargarConsumo(p.id);
}

function htmlCarta() {
  const platos = estado.menu.filter((pl) => estado.categoria === 'TODO' || pl.tipo === estado.categoria);
  return `
    <div class="categorias" role="group" aria-label="Categorías">
      ${CATEGORIAS.map(([c, t]) => `<button data-categoria="${c}" aria-pressed="${estado.categoria === c}">${t}</button>`).join('')}
    </div>
    <div class="carta">
      ${platos.map((pl) => {
        const quedan = estado.disponibilidad[pl.nombre];
        const pocas = quedan !== undefined && quedan <= 5;
        return `
          <button class="plato" data-plato="${esc(pl.nombre)}" ${quedan === 0 ? 'disabled' : ''}>
            <b>${esc(pl.nombre)}</b>
            <span><span class="mono">${pesos(pl.precio)}</span>
              <span class="${pocas ? 'pocas' : ''}">${quedan === undefined ? '' : quedan === 0 ? 'Agotado' : `${quedan} porc.`}</span></span>
          </button>`;
      }).join('')}
    </div>`;
}

function htmlNueva(m) {
  const precio = (n) => estado.menu.find((pl) => pl.nombre === n)?.precio || 0;
  const lineas = [...estado.ticket];
  const total = lineas.reduce((s, [n, c]) => s + precio(n) * c, 0);
  return `
    <div class="tarjeta-cab">
      <div><div class="titulo-mesa">Mesa ${String(m.mesa).padStart(2, '0')}</div><span class="terciario">Nueva comanda</span></div>
      ${pastilla('Libre')}
    </div>
    <div class="panel-desplazable">
      ${htmlCarta()}
      <h3 class="subtitulo-seccion">Comanda</h3>
      ${lineas.length ? `<ul class="ticket">${lineas.map(([n, c]) => `
          <li>
            <span class="nombre">${esc(n)}<small>${pesos(precio(n))} c/u</small></span>
            <span class="cantidad">
              <button data-restar="${esc(n)}" aria-label="Quitar uno">−</button><span>${c}</span><button data-sumar="${esc(n)}" aria-label="Agregar uno">+</button>
            </span>
            <span class="importe mono">${pesos(precio(n) * c)}</span>
          </li>`).join('')}</ul>`
        : '<p class="vacio">Toca los platos de la carta para agregarlos.</p>'}
    </div>
    <div class="panel-acciones">
      <div class="total-linea"><span>Total</span><b class="mono">${pesos(total)}</b></div>
      <button class="btn btn-primario btn-bloque" data-accion="confirmar" ${lineas.length ? '' : 'disabled'}>
        Enviar a cocina ${icono.flecha}
      </button>
    </div>`;
}

function htmlPedido(m, p) {
  const idx = FLUJO.indexOf(p.estado);
  const siguiente = ESTADOS[p.estado]?.siguiente;
  const editable = p.estado === 'Creado' || p.estado === 'En preparación';
  return `
    <div class="tarjeta-cab">
      <div><div class="titulo-mesa">Mesa ${String(m.mesa).padStart(2, '0')}</div>
        <span class="terciario">Pedido #${p.id} · ${transcurrido(p.horaCreacion) === 'ahora' ? 'recién creado' : `hace ${transcurrido(p.horaCreacion)}`}</span></div>
      ${pastilla(p.estado)}
    </div>
    <div class="panel-desplazable">
      <div class="pasos" style="--c: var(--${varEstado(p.estado)})">
        ${FLUJO.map((f, i) => `<div class="paso ${i <= idx ? 'hecho' : ''} ${i === idx ? 'actual' : ''}"><i></i>${esc(f)}</div>`).join('')}
      </div>
      <h3 class="subtitulo-seccion">Consumo</h3>
      <ul class="ticket">${p.lineas.map((l) => `
        <li>
          <span class="nombre">${esc(l.plato)}<small>${l.cantidad} × ${pesos(l.precioUnitario)}</small></span>
          ${editable && p.lineas.length > 1
            ? `<button class="btn btn-fantasma btn-icono" data-quitar="${l.linea}" aria-label="Quitar ${esc(l.plato)}">${icono.x}</button>`
            : '<span></span>'}
          <span class="importe mono">${pesos(l.subtotal)}</span>
        </li>`).join('')}</ul>
      ${editable ? `
        <div style="padding: 4px 18px 0">
          <button class="btn btn-fantasma" data-accion="agregar">${estado.agregando ? icono.x + ' Cerrar carta' : icono.mas + ' Agregar platos'}</button>
        </div>
        ${estado.agregando ? htmlCarta() : ''}` : ''}
      ${estado.consumo.id === p.id && estado.consumo.lista.length ? `
        <h3 class="subtitulo-seccion">Ingredientes descontados</h3>
        <ul class="consumo">${estado.consumo.lista.map((c) => `<li>${esc(c)}</li>`).join('')}</ul>` : ''}
    </div>
    <div class="panel-acciones">
      <div class="total-linea"><span>Total</span><b class="mono">${pesos(p.total)}</b></div>
      ${siguiente ? `<button class="btn btn-primario btn-bloque" data-accion="avanzar">${esc(siguiente)} ${icono.flecha}</button>` : ''}
      <div class="fila-acciones">
        <button class="btn" data-accion="dividir">${icono.dividir} Dividir cuenta</button>
        <button class="btn btn-peligro" data-accion="cancelar">Cancelar pedido</button>
      </div>
    </div>`;
}

async function cargarConsumo(id) {
  estado.consumo = { id, lista: [] };
  try {
    const movs = await api.consumoPedido(id);
    const suma = new Map();
    for (const mv of movs) {
      const signo = { SALIDA_VENTA: 1, DEVOLUCION: -1 }[mv.tipo] || 0;
      suma.set(mv.ingrediente, (suma.get(mv.ingrediente) || 0) + signo * mv.cantidad);
    }
    estado.consumo = { id, lista: [...suma].filter(([, c]) => c > 0).map(([ing, c]) => `${estado.nombresIngredientes[ing] || ing} × ${c}`) };
    pintarPanel();
  } catch { /* el detalle de consumo es opcional */ }
}

// ---------- Acciones ----------

async function clicPanel(ev) {
  const t = ev.target.closest('button');
  if (!t || t.disabled) return;
  const m = mesaSel();
  const p = pedidoDeMesa(m);

  if (t.dataset.categoria) { estado.categoria = t.dataset.categoria; return pintarPanel(); }
  if (t.dataset.plato) {
    if (p) {
      const r = await intentar(() => api.agregarItem(p.id, t.dataset.plato, 1), `${t.dataset.plato} agregado`);
      if (r) { estado.consumo.id = null; await salon.refrescar(); }
      return;
    }
    estado.ticket.set(t.dataset.plato, (estado.ticket.get(t.dataset.plato) || 0) + 1);
    return pintarPanel();
  }
  if (t.dataset.sumar) { estado.ticket.set(t.dataset.sumar, estado.ticket.get(t.dataset.sumar) + 1); return pintarPanel(); }
  if (t.dataset.restar) {
    const c = estado.ticket.get(t.dataset.restar) - 1;
    if (c <= 0) estado.ticket.delete(t.dataset.restar); else estado.ticket.set(t.dataset.restar, c);
    return pintarPanel();
  }
  if (t.dataset.quitar !== undefined) {
    const r = await intentar(() => api.quitarLinea(p.id, Number(t.dataset.quitar)), 'Línea quitada');
    if (r) { estado.consumo.id = null; await salon.refrescar(); }
    return;
  }

  switch (t.dataset.accion) {
    case 'confirmar': {
      t.disabled = true;
      const lineas = [...estado.ticket].map(([plato, cantidad]) => ({ plato, cantidad }));
      const r = await intentar(() => api.crearPedido(m.mesa, lineas), `Comanda de la mesa ${m.mesa} enviada a cocina`);
      if (r) estado.ticket = new Map();
      t.disabled = false;
      return salon.refrescar();
    }
    case 'agregar':
      estado.agregando = !estado.agregando;
      return pintarPanel();
    case 'avanzar': {
      t.disabled = true;
      const r = await intentar(() => api.avanzar(p.id));
      if (r) avisarEstado(r);
      return salon.refrescar();
    }
    case 'dividir':
      return abrirDivision(p);
    case 'cancelar': {
      const ok = await confirmar(`¿Cancelar el pedido #${p.id}?`,
        'Los ingredientes que no se alcanzaron a preparar vuelven al inventario.', 'Cancelar pedido', true);
      if (ok) {
        await intentar(() => api.cancelar(p.id), `Pedido #${p.id} cancelado`);
        await salon.refrescar();
      }
      return undefined;
    }
    default:
      return undefined;
  }
}

function avisarEstado(p) {
  avisar(`Mesa ${p.mesa}: ${p.estado}`, p.estado === 'Entregado' ? 'La mesa quedó libre.' : '');
}
