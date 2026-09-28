import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { BASE_URL, SCENARIO, JSON_HEADERS, resumen } from './comun.js';

/**
 * PRUEBA DE CARGA - Retos 1 y 2 (y el reto implícito de concurrencia: varios
 * meseros tomando pedidos al mismo tiempo).
 *
 * Cada usuario virtual (VU) es un mesero que atiende SU mesa (mesa = número
 * de VU) y repite el ciclo completo:
 *   1. toma un pedido de 1 a 3 platos   -> descuenta inventario (Reto 1)
 *   2. lo avanza 3 veces hasta Entregado
 *   3. divide la cuenta entre 1 y 4 personas con 10 % de propina (Reto 2)
 *
 * SLO (definidos ANTES de ejecutar, ver perf/README.md):
 *   - p95 de toda petición          <= 500 ms
 *   - p95 crear pedido               <= 500 ms  (toca pedidos + inventario en transacción)
 *   - p95 avanzar / dividir cuenta   <= 300 ms
 *   - errores HTTP                   <  1 %
 *   - checks de negocio              >  99 %  (201, estado Entregado, la cuenta cuadra)
 *
 * Arranque del servicio (sin ventana y con suficientes mesas para los VUs):
 *   java -jar target/orderflow-analytics-2.0.0.jar --orderflow.ui.enabled=false --orderflow.mesas=1000
 * Ejecución (desde la raíz del repositorio):
 *   k6 run -e SCENARIO=baseline perf/scripts/flujo_pedidos_k6.js
 *   k6 run -e SCENARIO=carga    perf/scripts/flujo_pedidos_k6.js
 */

const ESCENARIOS = {
  baseline: { executor: 'constant-vus', vus: 10, duration: '1m' },
  carga: {
    executor: 'ramping-vus', startVUs: 0, gracefulRampDown: '20s',
    stages: [{ duration: '30s', target: 50 }, { duration: '3m', target: 50 }, { duration: '30s', target: 0 }],
  },
  estres: {
    executor: 'ramping-vus', startVUs: 50, gracefulRampDown: '20s',
    stages: [{ duration: '2m', target: 200 }, { duration: '1m', target: 200 }, { duration: '30s', target: 0 }],
  },
  pico: {
    executor: 'ramping-vus', startVUs: 10, gracefulRampDown: '20s',
    stages: [{ duration: '15s', target: 150 }, { duration: '30s', target: 150 }, { duration: '30s', target: 10 },
      { duration: '30s', target: 10 }],
  },
};

const SLO = [['crear_pedido', 500], ['avanzar_pedido', 300], ['dividir_cuenta', 300]];

export const options = {
  scenarios: { run: ESCENARIOS[SCENARIO] || ESCENARIOS.baseline },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    'http_req_duration{name:crear_pedido}': ['p(95)<500'],
    'http_req_duration{name:avanzar_pedido}': ['p(95)<300'],
    'http_req_duration{name:dividir_cuenta}': ['p(95)<300'],
    checks: ['rate>0.99'],
  },
};

const stockInsuficiente = new Counter('pedidos_rechazados_por_stock');
const cuentaCuadra = new Rate('cuenta_cuadra');

/** Antes de empezar: surte el inventario para que la prueba mida rendimiento, no escasez. */
export function setup() {
  const inv = http.get(`${BASE_URL}/api/inventario`).json();
  for (const ing of inv) {
    http.post(`${BASE_URL}/api/inventario/${ing.codigo}/reposicion`,
      JSON.stringify({ cantidad: 50000000, nota: 'Surtido para prueba de carga k6' }), JSON_HEADERS);
  }
  const menu = http.get(`${BASE_URL}/api/menu`).json().map((p) => p.nombre);
  return { menu };
}

export default function (datos) {
  const mesa = __VU;
  const lineas = [];
  const n = 1 + Math.floor(Math.random() * 3);
  for (let i = 0; i < n; i++) {
    lineas.push({ plato: datos.menu[Math.floor(Math.random() * datos.menu.length)], cantidad: 1 + Math.floor(Math.random() * 2) });
  }

  // 1. Tomar el pedido
  const creado = http.post(`${BASE_URL}/api/pedidos`, JSON.stringify({ mesa, lineas }),
    Object.assign({ tags: { name: 'crear_pedido' } }, JSON_HEADERS));
  if (creado.status === 409 && String(creado.body).includes('Stock')) {
    stockInsuficiente.add(1);
  }
  if (!check(creado, { 'pedido creado (201)': (r) => r.status === 201 })) {
    return;
  }
  const id = creado.json('id');

  // 2. Cocina y mesero lo avanzan hasta Entregado
  let estado = '';
  for (let i = 0; i < 3; i++) {
    const r = http.post(`${BASE_URL}/api/pedidos/${id}/avanzar`, null,
      Object.assign({ tags: { name: 'avanzar_pedido' } }, JSON_HEADERS));
    estado = r.status === 200 ? r.json('estado') : `HTTP ${r.status}`;
  }
  check(estado, { 'pedido entregado': (e) => e === 'Entregado' });

  // 3. La mesa pide la cuenta dividida
  const personas = 1 + Math.floor(Math.random() * 4);
  const div = http.post(`${BASE_URL}/api/pedidos/${id}/division`,
    JSON.stringify({ metodo: 'IGUALITARIA', numeroPersonas: personas, propinaPorcentaje: 10 }),
    Object.assign({ tags: { name: 'dividir_cuenta' } }, JSON_HEADERS));
  const ok = div.status === 200 && div.json('partes').reduce((s, p) => s + p.total, 0) === div.json('total');
  cuentaCuadra.add(ok);
  check(div, { 'cuenta dividida y cuadra al peso': () => ok });

  const pausa = Number(__ENV.SLEEP_MS || 0);
  if (pausa > 0) sleep(pausa / 1000);
}

/** Al final: ningún ingrediente puede haber quedado negativo (evidencia de consistencia). */
export function teardown() {
  const inv = http.get(`${BASE_URL}/api/inventario`).json();
  const negativos = inv.filter((i) => i.stock < 0);
  console.log(`Verificación de inventario: ${inv.length} ingredientes, ${negativos.length} con stock negativo`);
}

export function handleSummary(data) {
  return resumen(data, 'flujo-pedidos', SLO);
}
