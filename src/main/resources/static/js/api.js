// Cliente de la API REST. La web es otro adaptador de entrada: todo lo que
// hace pasa por los mismos endpoints que usan k6 y las pruebas de sistema.

export class ErrorApi extends Error {
  constructor(status, cuerpo) {
    super((cuerpo && cuerpo.mensaje) || `Error ${status}`);
    this.status = status;
    this.faltantes = (cuerpo && cuerpo.faltantes) || [];
  }
}

function leerCookie(nombre) {
  const par = document.cookie.split('; ').find((c) => c.startsWith(`${nombre}=`));
  return par ? decodeURIComponent(par.slice(nombre.length + 1)) : null;
}

/** Cabeceras comunes; las peticiones que cambian datos llevan el token CSRF (cookie XSRF-TOKEN). */
function cabeceras(metodo) {
  const h = { Accept: 'application/json' };
  const token = metodo === 'GET' ? null : leerCookie('XSRF-TOKEN');
  if (token) h['X-XSRF-TOKEN'] = token;
  return h;
}

async function leer(resp, ruta) {
  const texto = await resp.text();
  let datos = null;
  try { datos = texto ? JSON.parse(texto) : null; } catch { datos = null; }
  if (resp.status === 401 && !ruta.startsWith('/sesion')) window.dispatchEvent(new CustomEvent('sesion-expirada'));
  if (!resp.ok) throw new ErrorApi(resp.status, datos);
  return datos;
}

async function pedir(metodo, ruta, cuerpo) {
  const opciones = { method: metodo, headers: cabeceras(metodo), credentials: 'same-origin' };
  if (cuerpo !== undefined) {
    opciones.headers['Content-Type'] = 'application/json';
    opciones.body = JSON.stringify(cuerpo);
  }
  return leer(await fetch(`/api${ruta}`, opciones), ruta);
}

/** Inicia sesión con usuario y clave; la cookie de sesión queda en el navegador (HttpOnly). */
async function iniciarSesion(usuario, clave) {
  if (!leerCookie('XSRF-TOKEN')) {
    await fetch('/api/sesion', { credentials: 'same-origin' });   // deja la cookie XSRF-TOKEN
  }
  const resp = await fetch('/api/sesion/login', {
    method: 'POST',
    credentials: 'same-origin',
    headers: { ...cabeceras('POST'), 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ usuario, clave }),
  });
  return leer(resp, '/sesion/login');
}

const get = (ruta) => pedir('GET', ruta);
const post = (ruta, cuerpo) => pedir('POST', ruta, cuerpo);

export const api = {
  sesion: () => get('/sesion'),
  iniciarSesion,
  cerrarSesion: () => pedir('POST', '/sesion/logout'),
  menu: () => get('/menu'),
  mesas: () => get('/mesas'),
  pedidos: () => get('/pedidos'),
  pedido: (id) => get(`/pedidos/${id}`),
  crearPedido: (mesa, lineas) => post('/pedidos', { mesa, lineas }),
  agregarItem: (id, plato, cantidad) => post(`/pedidos/${id}/items`, { plato, cantidad }),
  quitarLinea: (id, linea) => pedir('DELETE', `/pedidos/${id}/items/${linea}`),
  avanzar: (id) => post(`/pedidos/${id}/avanzar`),
  cancelar: (id) => post(`/pedidos/${id}/cancelar`),
  dividir: (id, solicitud) => post(`/pedidos/${id}/division`, solicitud),
  consumoPedido: (id) => get(`/pedidos/${id}/inventario`),

  inventario: () => get('/inventario'),
  movimientos: (codigo, limite = 40) =>
    get(`/inventario/movimientos?limite=${limite}${codigo ? `&ingrediente=${encodeURIComponent(codigo)}` : ''}`),
  disponibilidad: () => get('/menu/disponibilidad'),
  reponer: (codigo, cantidad, nota) => post(`/inventario/${encodeURIComponent(codigo)}/reposicion`, { cantidad, nota }),
  ajustar: (codigo, stockContado, nota) => post(`/inventario/${encodeURIComponent(codigo)}/ajuste`, { stockContado, nota }),

  analitica: (periodo) => get(`/analitica?periodo=${periodo}`),
  notificaciones: () => get('/notificaciones'),
};
