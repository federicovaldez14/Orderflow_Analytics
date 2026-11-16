import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, sesionJson, iniciarSesion, esperarServicio } from './comun.js';

/**
 * PRUEBA DE HUMO del entorno desplegado (etapa "Despliegue y verificación").
 *
 * Recorre UNA vez el flujo principal del sistema, como lo haría el
 * restaurante al abrir: salud -> iniciar sesión -> carta -> tomar un pedido
 * (descuenta inventario) -> llevarlo hasta Entregado -> dividir la cuenta ->
 * analítica -> cerrar sesión. Si cualquier paso falla, el umbral checks==100 %
 * no se cumple y el pipeline se detiene.
 *
 *   k6 run -e BASE_URL=https://... -e ORDERFLOW_CLAVE=<clave-admin> perf/scripts/humo_k6.js
 */
export const options = {
  vus: 1,
  iterations: 1,
  noCookiesReset: true,
  thresholds: {
    checks: ['rate==1.0'],
    http_req_failed: ['rate==0'],
  },
};

/** Mesa reservada para la prueba de humo (la de carga usa las mesas 1 a 5). */
const MESA = Number(__ENV.MESA_HUMO || 10);

export function setup() {
  esperarServicio();
}

export default function () {
  check(http.get(`${BASE_URL}/actuator/health/readiness`), { 'servicio listo (readiness UP)': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/`), { 'la web carga (index.html)': (r) => r.status === 200 && r.body.includes('Orderflow') });

  iniciarSesion();
  check(http.get(`${BASE_URL}/api/sesion`), { 'sesión iniciada como administrador': (r) => r.status === 200 && r.json('roles').includes('ADMIN') });

  const menu = http.get(`${BASE_URL}/api/menu`);
  check(menu, { 'carta disponible': (r) => r.status === 200 && r.json().length > 0 });

  // Si una corrida anterior dejó la mesa ocupada, se cancela ese pedido
  const mesa = http.get(`${BASE_URL}/api/mesas`).json().find((m) => m.mesa === MESA);
  if (mesa && !mesa.libre) {
    http.post(`${BASE_URL}/api/pedidos/${mesa.pedidoId}/cancelar`, null, sesionJson('cancelar'));
  }

  const plato = menu.json()[0].nombre;
  const creado = http.post(`${BASE_URL}/api/pedidos`, JSON.stringify({ mesa: MESA, lineas: [{ plato, cantidad: 2 }] }),
    sesionJson('crear_pedido'));
  if (!check(creado, { 'pedido creado (201) y descuenta inventario': (r) => r.status === 201 })) {
    console.error(`crear pedido: HTTP ${creado.status} ${String(creado.body).substring(0, 200)}`);
    return;
  }
  const id = creado.json('id');
  check(http.get(`${BASE_URL}/api/pedidos/${id}/inventario`), {
    'el pedido dejó movimientos de inventario (trazabilidad)': (r) => r.status === 200 && r.json().length > 0,
  });

  let estado = '';
  for (let i = 0; i < 3; i++) {
    estado = http.post(`${BASE_URL}/api/pedidos/${id}/avanzar`, null, sesionJson('avanzar_pedido')).json('estado');
  }
  check(estado, { 'pedido entregado': (e) => e === 'Entregado' });

  const division = http.post(`${BASE_URL}/api/pedidos/${id}/division`,
    JSON.stringify({ metodo: 'IGUALITARIA', numeroPersonas: 3, propinaPorcentaje: 10 }), sesionJson('dividir_cuenta'));
  check(division, {
    'cuenta dividida y cuadra al peso': (r) => r.status === 200
      && r.json('partes').reduce((s, p) => s + p.total, 0) === r.json('total'),
  });

  check(http.get(`${BASE_URL}/api/analitica?periodo=HOY`), { 'analítica responde con los 6 reportes': (r) => r.status === 200 && r.json('reportes').length === 6 });
  check(http.post(`${BASE_URL}/api/sesion/logout`, null, sesionJson('logout')), { 'cierre de sesión': (r) => r.status === 204 });
}

export function handleSummary(data) {
  const checks = data.root_group.checks.map((c) => `| ${c.fails === 0 ? '✅' : '❌'} | ${c.name} |`).join('\n');
  const md = `# Prueba de humo\n\nEntorno: ${BASE_URL}\n\nFecha: ${new Date().toISOString()}\n\n| | Verificación |\n|---|---|\n${checks}\n`;
  return { 'perf/results/humo.md': md, stdout: md };
}
