import { api } from './api.js';
import { esc, pesos, abrirDialogo, intentar, icono } from './ui.js';

// Diálogo del Reto 2. La web solo arma la solicitud; el reparto exacto al
// peso (y la validación de que todo cuadre) lo hace el dominio.
const METODOS = [['IGUALITARIA', 'Partes iguales'], ['POR_CONSUMO', 'Por consumo'], ['POR_PORCENTAJE', 'Por porcentaje']];
const PROPINAS = [0, 10, 15];

export function abrirDivision(pedido) {
  const s = {
    metodo: 'IGUALITARIA',
    numero: 2,
    personas: ['Persona 1', 'Persona 2'],
    pesos: pedido.lineas.map(() => [1, 1]),
    porcentajes: [50, 50],
    propina: 10,
  };

  const d = abrirDialogo(`
    <div class="dialogo-cab">
      <div><h2>Dividir la cuenta</h2><p>Mesa ${pedido.mesa} · Pedido #${pedido.id} · Consumo ${pesos(pedido.total)}</p></div>
      <button class="btn btn-fantasma btn-icono" data-cerrar aria-label="Cerrar">${icono.x}</button>
    </div>
    <div class="dialogo-cuerpo">
      <div class="segmentado" role="group" aria-label="Método" id="metodos">
        ${METODOS.map(([m, t]) => `<button data-metodo="${m}" aria-pressed="${m === s.metodo}">${t}</button>`).join('')}
      </div>
      <div id="config"></div>
      <div class="campo"><label>Propina voluntaria</label>
        <div class="propinas" id="propinas">
          ${PROPINAS.map((p) => `<button class="btn" data-propina="${p}">${p} %</button>`).join('')}
          <input class="entrada mono" id="propina-otra" type="number" min="0" max="100" aria-label="Otro porcentaje de propina" placeholder="Otro %">
        </div>
      </div>
      <div id="resultado"></div>
    </div>
    <div class="dialogo-pie">
      <button class="btn btn-fantasma" data-cerrar>Cerrar</button>
      <button class="btn btn-primario" id="calcular">Calcular división</button>
    </div>`);

  const config = d.querySelector('#config');
  const resultado = d.querySelector('#resultado');

  function marcarPropina() {
    d.querySelectorAll('[data-propina]').forEach((b) => {
      b.classList.toggle('btn-primario', Number(b.dataset.propina) === s.propina);
    });
  }

  function sincronizarPersonas() {
    while (s.porcentajes.length < s.personas.length) s.porcentajes.push(0);
    s.porcentajes.length = s.personas.length;
    s.pesos = s.pesos.map((fila) => {
      const f = fila.slice(0, s.personas.length);
      while (f.length < s.personas.length) f.push(0);
      return f;
    });
  }

  function repartirPorcentajes() {
    const n = s.personas.length;
    const base = Math.floor(100 / n);
    s.porcentajes = s.personas.map((_, i) => (i === n - 1 ? 100 - base * (n - 1) : base));
  }

  function listaPersonas(conPorcentaje) {
    return `
      <div class="personas-lista">
        ${s.personas.map((p, i) => `
          <div class="persona-fila" ${conPorcentaje ? '' : 'style="grid-template-columns: 1fr auto"'}>
            <input class="entrada" data-nombre="${i}" value="${esc(p)}" aria-label="Nombre de la persona ${i + 1}" maxlength="30">
            ${conPorcentaje ? `<input class="entrada mono" type="number" min="1" max="100" data-porcentaje="${i}" value="${s.porcentajes[i]}" aria-label="Porcentaje de ${esc(p)}">` : ''}
            <button class="btn btn-fantasma btn-icono" data-quitar-persona="${i}" ${s.personas.length <= 2 ? 'disabled' : ''} aria-label="Quitar persona">${icono.x}</button>
          </div>`).join('')}
        <div><button class="btn btn-fantasma" data-agregar-persona>${icono.mas} Agregar persona</button></div>
      </div>`;
  }

  function pintarSumaPorcentajes() {
    const el = d.querySelector('#suma');
    if (!el) return;
    const suma = s.porcentajes.reduce((a, b) => a + (Number(b) || 0), 0);
    el.textContent = suma === 100 ? 'Suman 100 %' : `Suman ${suma} % (deben sumar 100)`;
    el.className = `suma ${suma === 100 ? 'ok' : 'mal'}`;
    // Evaluación UX, hallazgo H5-2: no se deja calcular hasta que los porcentajes sumen 100.
    d.querySelector('#calcular').disabled = suma !== 100;
  }

  function pintarConfig() {
    if (s.metodo === 'IGUALITARIA') {
      config.innerHTML = `
        <div class="campo"><label>Número de personas</label>
          <span class="cantidad" style="width:max-content">
            <button data-numero="-1" aria-label="Una persona menos">−</button><span class="mono">${s.numero}</span><button data-numero="1" aria-label="Una persona más">+</button>
          </span>
          <span class="terciario">Cada quien paga ${pesos(Math.ceil(pedido.total / s.numero))} aprox. antes de propina.</span>
        </div>`;
    } else if (s.metodo === 'POR_CONSUMO') {
      config.innerHTML = `
        <p class="secundario" style="margin:0">Indica cuántas partes de cada plato le tocan a cada persona.
          Con 1 y 1 lo comparten a mitades; deja 0 si no lo consumió.</p>
        <div class="tabla-envoltura">
          <table class="matriz">
            <thead><tr><th>Plato</th>${s.personas.map((p, i) => `<th><input class="entrada" data-nombre="${i}" value="${esc(p)}" aria-label="Nombre de la persona ${i + 1}" maxlength="30"></th>`).join('')}</tr></thead>
            <tbody>${pedido.lineas.map((l, li) => `
              <tr><td><b>${l.cantidad}× ${esc(l.plato)}</b><div class="terciario mono">${pesos(l.subtotal)}</div></td>
                ${s.personas.map((_, pi) => `<td><input class="entrada mono" type="number" min="0" max="99" data-peso="${li}-${pi}" value="${s.pesos[li][pi]}" aria-label="Partes de ${esc(l.plato)} para ${esc(s.personas[pi])}"></td>`).join('')}
              </tr>`).join('')}</tbody>
          </table>
        </div>
        <div style="display:flex;gap:8px">
          <button class="btn btn-fantasma" data-agregar-persona>${icono.mas} Agregar persona</button>
          ${s.personas.length > 2 ? `<button class="btn btn-fantasma" data-quitar-persona="${s.personas.length - 1}">${icono.x} Quitar la última</button>` : ''}
        </div>`;
    } else {
      config.innerHTML = `${listaPersonas(true)}<div id="suma" class="suma"></div>`;
      pintarSumaPorcentajes();
    }
  }

  function solicitud() {
    const base = { metodo: s.metodo, propinaPorcentaje: s.propina };
    const nombres = s.personas.map((p) => p.trim());
    if (s.metodo === 'IGUALITARIA') return { ...base, numeroPersonas: s.numero };
    if (s.metodo === 'POR_CONSUMO') {
      return {
        ...base,
        asignaciones: pedido.lineas.map((l, li) => ({
          linea: l.linea,
          personas: Object.fromEntries(nombres.map((n, pi) => [n, Number(s.pesos[li][pi]) || 0]).filter(([, w]) => w > 0)),
        })),
      };
    }
    return { ...base, porcentajes: Object.fromEntries(nombres.map((n, i) => [n, Number(s.porcentajes[i]) || 0])) };
  }

  function pintarResultado(r) {
    const suma = r.partes.reduce((a, p) => a + p.total, 0);
    resultado.innerHTML = `
      <div class="partes">
        ${r.partes.map((p) => `
          <div class="parte">
            <h4>${esc(p.persona)}</h4>
            <b class="mono">${pesos(p.total)}</b>
            <dl><dt>Consumo</dt><dd class="mono">${pesos(p.consumo)}</dd><dt>Propina</dt><dd class="mono">${pesos(p.propina)}</dd></dl>
            <p>${esc(p.detalle.join(' · '))}</p>
          </div>`).join('')}
      </div>
      <div class="verificacion" style="margin-top:12px">${icono.ok.replace('<svg', '<svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4"')}
        Total ${pesos(r.total)} (consumo ${pesos(r.subtotal)} + propina ${pesos(r.propina)}) = suma de las partes ${pesos(suma)}</div>`;
  }

  d.addEventListener('click', async (ev) => {
    const b = ev.target.closest('button');
    if (!b || b.disabled) return;
    if (b.dataset.metodo) {
      s.metodo = b.dataset.metodo;
      d.querySelector('#calcular').disabled = false;
      d.querySelectorAll('[data-metodo]').forEach((x) => x.setAttribute('aria-pressed', x === b));
      resultado.innerHTML = '';
      pintarConfig();
    } else if (b.dataset.numero) {
      s.numero = Math.min(20, Math.max(1, s.numero + Number(b.dataset.numero)));
      pintarConfig();
    } else if (b.dataset.propina !== undefined) {
      s.propina = Number(b.dataset.propina);
      d.querySelector('#propina-otra').value = '';
      marcarPropina();
    } else if (b.hasAttribute('data-agregar-persona')) {
      s.personas.push(`Persona ${s.personas.length + 1}`);
      sincronizarPersonas();
      repartirPorcentajes();
      pintarConfig();
    } else if (b.dataset.quitarPersona !== undefined) {
      s.personas.splice(Number(b.dataset.quitarPersona), 1);
      s.pesos = s.pesos.map((f) => f.filter((_, i) => i !== Number(b.dataset.quitarPersona)));
      sincronizarPersonas();
      repartirPorcentajes();
      pintarConfig();
    } else if (b.id === 'calcular') {
      b.disabled = true;
      const r = await intentar(() => api.dividir(pedido.id, solicitud()));
      b.disabled = false;
      if (r) {
        pintarResultado(r);
        resultado.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    }
  });

  d.addEventListener('input', (ev) => {
    const t = ev.target;
    if (t.dataset.nombre !== undefined) s.personas[Number(t.dataset.nombre)] = t.value;
    if (t.dataset.porcentaje !== undefined) { s.porcentajes[Number(t.dataset.porcentaje)] = Number(t.value); pintarSumaPorcentajes(); }
    if (t.dataset.peso) { const [li, pi] = t.dataset.peso.split('-').map(Number); s.pesos[li][pi] = Number(t.value); }
    if (t.id === 'propina-otra' && t.value !== '') { s.propina = Number(t.value); marcarPropina(); }
  });

  marcarPropina();
  pintarConfig();
}
