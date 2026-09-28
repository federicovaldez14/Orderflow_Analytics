import { api } from '../api.js';
import { esc, icono } from '../ui.js';

// Las líneas vienen del Observer del dominio con el formato
// "[HH:mm:ss] COCINA | Pedido #12 (mesa 3): mensaje" o, en stock,
// "[HH:mm:ss] STOCK BAJO: ...". Se separan hora y texto para mostrarlas.
const CANALES = [
  ['cocina', 'Cocina', 'Todo lo que pasa con cada comanda.'],
  ['mesero', 'Mesero', 'Avisos del pedido para quien atiende la mesa.'],
  ['inventario', 'Stock', 'Ingredientes que bajaron del mínimo.'],
];

let raiz;
let firma = '';

export const actividad = {
  titulo: 'Actividad',
  async montar(contenedor) {
    raiz = contenedor;
    firma = '';
    raiz.innerHTML = `
      <header class="encabezado">
        <div><h1>Actividad</h1><p>Notificaciones en vivo de cada canal (Observer del dominio), de la más reciente a la más antigua.</p></div>
      </header>
      <div class="actividad" id="canales"></div>`;
    await this.refrescar();
  },
  async refrescar() {
    const datos = await api.notificaciones();
    const nueva = JSON.stringify(datos);
    if (nueva === firma) return;
    firma = nueva;
    raiz.querySelector('#canales').innerHTML = CANALES.map(([clave, titulo, ayuda]) => {
      const lineas = [...(datos[clave] || [])].reverse().slice(0, 80);
      return `
        <section class="tarjeta">
          <div class="tarjeta-cab"><div><h2>${titulo}</h2><p class="terciario" style="margin:3px 0 0;font-size:12px">${ayuda}</p></div>
            <span class="insignia">${(datos[clave] || []).length}</span></div>
          <ul class="feed">${lineas.length ? lineas.map(linea).join('') : `<li class="vacio" style="display:block">${icono.vacio}Sin novedades</li>`}</ul>
        </section>`;
    }).join('');
  },
};

function linea(texto) {
  const m = texto.match(/^\[(\d{2}:\d{2}):\d{2}\]\s*(?:[A-ZÁÉÍÓÚ ]+\|\s*)?(.*)$/);
  const hora = m ? m[1] : '';
  let cuerpo = m ? m[2] : texto;
  let titulo = '';
  const p = cuerpo.match(/^(Pedido #\d+ \(mesa \d+\)):\s*(.*)$/);
  if (p) { titulo = p[1]; cuerpo = p[2]; }
  const s = cuerpo.match(/^STOCK BAJO:\s*(.*)$/);
  if (s) { titulo = 'Stock bajo'; cuerpo = s[1]; }
  return `<li><span class="hora mono">${esc(hora)}</span><span class="texto">${titulo ? `<b>${esc(titulo)}</b>` : ''}<span>${esc(cuerpo)}</span></span></li>`;
}
