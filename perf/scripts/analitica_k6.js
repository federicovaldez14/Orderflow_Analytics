import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, SCENARIO, resumen } from './comun.js';

/**
 * PRUEBA DE CARGA - Reto 3 (analítica).
 *
 * Varios usuarios (dueño, administrador, pantallas) consultan el panel de
 * analítica a la vez. El panel se calcula leyendo TODOS los pedidos en cada
 * consulta, así que su costo crece con el historial: esta prueba mide si
 * aguanta y deja ver ese cuello de botella.
 *
 * SLO (definidos antes de ejecutar):
 *   - p95 GET /api/analitica  <= 500 ms
 *   - errores HTTP            <  1 %
 *
 * Ejecución:
 *   k6 run -e SCENARIO=baseline perf/scripts/analitica_k6.js
 *   k6 run -e SCENARIO=carga    perf/scripts/analitica_k6.js
 * Para ver el efecto del historial, córrala antes y después de
 * flujo_pedidos_k6.js (que agrega miles de pedidos).
 */

const ESCENARIOS = {
  baseline: { executor: 'constant-vus', vus: 5, duration: '1m' },
  carga: {
    executor: 'ramping-vus', startVUs: 0, gracefulRampDown: '20s',
    stages: [{ duration: '30s', target: 30 }, { duration: '2m', target: 30 }, { duration: '30s', target: 0 }],
  },
};

const SLO = [['analitica_panel', 500]];

export const options = {
  scenarios: { run: ESCENARIOS[SCENARIO] || ESCENARIOS.baseline },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:analitica_panel}': ['p(95)<500'],
    checks: ['rate>0.99'],
  },
};

const PERIODOS = ['HOY', 'SEMANA', 'TODO'];

export default function () {
  const periodo = PERIODOS[Math.floor(Math.random() * PERIODOS.length)];
  const r = http.get(`${BASE_URL}/api/analitica?periodo=${periodo}`, { tags: { name: 'analitica_panel' }, timeout: '5s' });
  check(r, {
    'status 200': (x) => x.status === 200,
    'trae los 6 reportes': (x) => x.status === 200 && x.json('reportes').length === 6,
  });
}

export function handleSummary(data) {
  return resumen(data, 'analitica', SLO);
}
