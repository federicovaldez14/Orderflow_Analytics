// Utilidades compartidas por los scripts de carga de Orderflow Analytics.
// Mismo enfoque del taller de pruebas de carga: escenarios por variable de
// entorno, umbrales = SLO definidos ANTES de ejecutar y resumen en archivo.

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const SCENARIO = (__ENV.SCENARIO || 'baseline').toLowerCase();
export const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' }, timeout: '5s' };

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
