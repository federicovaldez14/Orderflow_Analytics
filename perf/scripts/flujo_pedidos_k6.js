import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { BASE_URL, SCENARIO, basico, sesionJson, iniciarSesion, esperarServicio, resumen } from './comun.js';

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
 * Cada VU inicia sesión una vez (cookie + token CSRF), como un mesero en la web.
 * Arranque del servicio (sin ventana y con suficientes mesas para los VUs):
 *   java -jar target/orderflow-analytics-3.0.0.jar --orderflow.ui.enabled=false --orderflow.mesas=1000
 * Ejecución (desde la raíz del repositorio):
 *   k6 run -e SCENARIO=baseline -e ORDERFLOW_CLAVE=<clave-admin> perf/scripts/flujo_pedidos_k6.js
 *   k6 run -e SCENARIO=carga    -e ORDERFLOW_CLAVE=<clave-admin> perf/scripts/flujo_pedidos_k6.js
 * Después de cada despliegue el pipeline corre la versión corta contra el entorno desplegado:
 *   k6 run -e SCENARIO=despliegue -e BASE_URL=https://... -e ORDERFLOW_CLAVE=... perf/scripts/flujo_pedidos_k6.js
 */

const ESCENARIOS = {
  // Prueba corta contra el entorno desplegado (pipeline de despliegue), mismos umbrales del SLO.
  // Representa la hora pico del restaurante con mucho margen: un flujo continuo que registra un pedido
  // completo (crear, avanzar hasta entregado y dividir la cuenta) cada ~2 s, unos 30 pedidos por
  // minuto, cuando 10 mesas generan menos de 1 por minuto. La capacidad máxima se mide aparte (carga,
  // estrés, pico); el plan gratuito de Render tiene 0,1 CPU y no está dimensionado para esas pruebas.
  despliegue: { executor: 'constant-vus', vus: 1, duration: '90s', startTime: '20s', pausaMs: 1500 },
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

/** Escenario elegido; el de despliegue va precedido de 20 s de calentamiento que no cuentan en el SLO. */
function escenarios() {
  const { pausaMs, ...run } = ESCENARIOS[SCENARIO] || ESCENARIOS.baseline;
  if (!run.startTime) return { run };
  return { calentamiento: { executor: 'constant-vus', vus: run.vus, duration: run.startTime }, run };
}

export const options = {
  // La sesión (cookies) se conserva entre iteraciones: cada VU inicia sesión una sola vez
  noCookiesReset: true,
  // En el plan gratuito de Render el servicio se duerme sin tráfico y tarda ≈ 1 min en despertar
  setupTimeout: '180s',
  scenarios: escenarios(),
  // Los umbrales solo miden el escenario "run": el calentamiento de la JVM recién desplegada no cuenta
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{scenario:run}': ['p(95)<500'],
    'http_req_duration{name:crear_pedido,scenario:run}': ['p(95)<500'],
    'http_req_duration{name:avanzar_pedido,scenario:run}': ['p(95)<300'],
    'http_req_duration{name:dividir_cuenta,scenario:run}': ['p(95)<300'],
    checks: ['rate>0.99'],
    // Reto 1: con todo el tráfico concurrente, ningún ingrediente puede quedar en negativo
    ingredientes_negativos: ['count==0'],
  },
};

const ESCENARIO = ESCENARIOS[SCENARIO] || ESCENARIOS.baseline;
const MAX_VUS = ESCENARIO.vus || Math.max(ESCENARIO.startVUs || 0, ...(ESCENARIO.stages || []).map((s) => s.target));
const ingredientesNegativos = new Counter('ingredientes_negativos');
let conSesion = false;   // por VU

const stockInsuficiente = new Counter('pedidos_rechazados_por_stock');
const cuentaCuadra = new Rate('cuenta_cuadra');

/** Antes de empezar: surte el inventario para que la prueba mida rendimiento, no escasez. */
export function setup() {
  esperarServicio();
  const admin = { headers: Object.assign({ 'Content-Type': 'application/json' }, basico()), timeout: '10s' };
  const inv = http.get(`${BASE_URL}/api/inventario`, admin).json();
  for (const ing of inv) {
    http.post(`${BASE_URL}/api/inventario/${ing.codigo}/reposicion`,
      JSON.stringify({ cantidad: 50000000, nota: 'Surtido para prueba de carga k6' }), admin);
  }
  // Mesas que usarán los VUs: si una corrida anterior dejó un pedido abierto, se cancela
  for (const m of http.get(`${BASE_URL}/api/mesas`, admin).json()) {
    if (!m.libre && m.mesa <= MAX_VUS) {
      http.post(`${BASE_URL}/api/pedidos/${m.pedidoId}/cancelar`, null, admin);
    }
  }
  const menu = http.get(`${BASE_URL}/api/menu`, admin).json().map((p) => p.nombre);
  return { menu };
}

export default function (datos) {
  if (!conSesion) {
    iniciarSesion();
    conSesion = true;
  }
  const mesa = __VU;
  const lineas = [];
  const n = 1 + Math.floor(Math.random() * 3);
  for (let i = 0; i < n; i++) {
    lineas.push({ plato: datos.menu[Math.floor(Math.random() * datos.menu.length)], cantidad: 1 + Math.floor(Math.random() * 2) });
  }

  // 1. Tomar el pedido
  const creado = http.post(`${BASE_URL}/api/pedidos`, JSON.stringify({ mesa, lineas }), sesionJson('crear_pedido'));
  if (creado.status === 409 && String(creado.body).includes('Stock')) {
    stockInsuficiente.add(1);
  }
  if (!check(creado, { 'pedido creado (201)': (r) => r.status === 201 })) {
    if (__ENV.DEBUG) console.warn(`crear_pedido mesa ${mesa}: HTTP ${creado.status} ${String(creado.body).substring(0, 200)}`);
    return;
  }
  const id = creado.json('id');

  // 2. Cocina y mesero lo avanzan hasta Entregado
  let estado = '';
  for (let i = 0; i < 3; i++) {
    const r = http.post(`${BASE_URL}/api/pedidos/${id}/avanzar`, null, sesionJson('avanzar_pedido'));
    estado = r.status === 200 ? r.json('estado') : `HTTP ${r.status}`;
  }
  check(estado, { 'pedido entregado': (e) => e === 'Entregado' });

  // 3. La mesa pide la cuenta dividida
  const personas = 1 + Math.floor(Math.random() * 4);
  const div = http.post(`${BASE_URL}/api/pedidos/${id}/division`,
    JSON.stringify({ metodo: 'IGUALITARIA', numeroPersonas: personas, propinaPorcentaje: 10 }),
    sesionJson('dividir_cuenta'));
  const ok = div.status === 200 && div.json('partes').reduce((s, p) => s + p.total, 0) === div.json('total');
  cuentaCuadra.add(ok);
  check(div, { 'cuenta dividida y cuadra al peso': () => ok });

  const pausa = Number(__ENV.SLEEP_MS || ESCENARIO.pausaMs || 0);
  if (pausa > 0) sleep(pausa / 1000);
}

/** Al final: ningún ingrediente puede haber quedado negativo (evidencia de consistencia). */
export function teardown() {
  const inv = http.get(`${BASE_URL}/api/inventario`, { headers: basico() }).json();
  const negativos = inv.filter((i) => i.stock < 0);
  ingredientesNegativos.add(negativos.length);
  console.log(`Verificación de inventario: ${inv.length} ingredientes, ${negativos.length} con stock negativo`);
}

export function handleSummary(data) {
  return resumen(data, 'flujo-pedidos', SLO);
}
