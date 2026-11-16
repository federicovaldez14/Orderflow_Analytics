// Utilidades compartidas por los scripts de carga de Orderflow Analytics.
// Mismo enfoque del taller de pruebas de carga: escenarios por variable de
// entorno, umbrales = SLO definidos ANTES de ejecutar y resumen en archivo.

import http from 'k6/http';
import encoding from 'k6/encoding';
import { sleep } from 'k6';

export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
export const SCENARIO = (__ENV.SCENARIO || 'baseline').toLowerCase();

// Credenciales: nunca en el script. En local salen del .env; en el pipeline, de GitHub Secrets.
const USUARIO = __ENV.ORDERFLOW_USUARIO || 'admin';
const CLAVE = __ENV.ORDERFLOW_CLAVE || '';

/** Cabecera HTTP Basic: solo para setup/teardown (pocas peticiones; BCrypt es costoso a propósito). */
export function basico() {
  if (!CLAVE) {
    throw new Error('Defina ORDERFLOW_CLAVE (clave del usuario con el que corre la prueba)');
  }
  return { Authorization: `Basic ${encoding.b64encode(`${USUARIO}:${CLAVE}`)}` };
}

function tokenCsrf() {
  const c = http.cookieJar().cookiesForURL(BASE_URL)['XSRF-TOKEN'];
  return c && c.length ? c[0] : '';
}

/** Cabeceras de una petición JSON con la sesión del VU (cookie) y el token CSRF. */
export function sesionJson(nombre) {
  return {
    headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': tokenCsrf() },
    tags: nombre ? { name: nombre } : {},
    timeout: '5s',
  };
}

/**
 * Inicia sesión una vez por VU, como lo haría un mesero en la web: la primera
 * visita deja la cookie XSRF-TOKEN y el login queda en la cookie de sesión.
 */
export function iniciarSesion() {
  // Sin sesión todavía responde 401 a propósito: es esperado y no cuenta como error
  http.get(`${BASE_URL}/api/sesion`, { tags: { name: 'sesion' }, responseCallback: http.expectedStatuses(200, 401) });
  const r = http.post(`${BASE_URL}/api/sesion/login`, { usuario: USUARIO, clave: CLAVE },
    { headers: { 'X-XSRF-TOKEN': tokenCsrf() }, tags: { name: 'login' }, timeout: '10s' });
  if (r.status !== 200) {
    throw new Error(`No se pudo iniciar sesión como ${USUARIO}: HTTP ${r.status}`);
  }
}

/** Espera a que el servicio responda (entornos gratuitos que se suspenden por inactividad). */
export function esperarServicio(maxSegundos = 180) {
  for (let i = 0; i < maxSegundos / 5; i++) {
    const r = http.get(`${BASE_URL}/actuator/health/readiness`, { timeout: '10s', tags: { name: 'salud' } });
    if (r.status === 200) {
      return;
    }
    sleep(5);
  }
  throw new Error(`El servicio no respondió /actuator/health/readiness en ${maxSegundos} s`);
}

/** Resumen legible (markdown) + JSON completo en perf/results/. */
export function resumen(data, prefijo, endpoints) {
  const m = data.metrics;
  const dur = (m.iteration_duration && m.iteration_duration.values) || {};
  const req = m.http_reqs ? m.http_reqs.values : { count: 0, rate: 0 };
  const fallidas = m.http_req_failed ? m.http_req_failed.values.rate : 0;
  const lineas = [];
  lineas.push(`# Resultado ${prefijo} - escenario ${SCENARIO}`);
  lineas.push('');
  lineas.push(`Fecha: ${new Date().toISOString()}`);
  lineas.push('');
  lineas.push('| Métrica | Valor |');
  lineas.push('|---|---|');
  lineas.push(`| Peticiones totales | ${req.count} |`);
  lineas.push(`| Throughput | ${req.rate.toFixed(1)} req/s |`);
  lineas.push(`| Tasa de error HTTP | ${(fallidas * 100).toFixed(2)} % |`);
  const d = m.http_req_duration.values;
  lineas.push(`| Latencia p50 / p90 / p95 / máx | ${d.med.toFixed(1)} / ${d['p(90)'].toFixed(1)} / ${d['p(95)'].toFixed(1)} / ${d.max.toFixed(1)} ms |`);
  if (dur.avg !== undefined) {
    lineas.push(`| Duración promedio de iteración | ${dur.avg.toFixed(1)} ms |`);
  }
  lineas.push('');
  lineas.push('## Latencia por endpoint (p95)');
  lineas.push('');
  lineas.push('| Endpoint | p95 (ms) | SLO |');
  lineas.push('|---|---|---|');
  for (const [nombre, slo] of endpoints) {
    const clave = `http_req_duration{name:${nombre}}`;
    if (m[clave]) {
      const p95 = m[clave].values['p(95)'];
      lineas.push(`| ${nombre} | ${p95.toFixed(1)} | ≤ ${slo} ms → ${p95 <= slo ? 'cumple' : 'NO cumple'} |`);
    }
  }
  lineas.push('');
  lineas.push('## Umbrales (SLO)');
  lineas.push('');
  for (const [nombre, metrica] of Object.entries(m)) {
    if (metrica.thresholds) {
      for (const [umbral, r] of Object.entries(metrica.thresholds)) {
        lineas.push(`- \`${nombre}\` ${umbral}: ${r.ok ? 'cumple' : 'NO cumple'}`);
      }
    }
  }
  const base = `perf/results/${prefijo}-${SCENARIO}`;
  return {
    [`${base}.json`]: JSON.stringify(data, null, 2),
    [`${base}.md`]: lineas.join('\n') + '\n',
    stdout: lineas.join('\n') + '\n',
  };
}
