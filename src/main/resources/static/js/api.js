// Cliente de la API REST. La web es otro adaptador de entrada: todo lo que
// hace pasa por los mismos endpoints que usan k6 y las pruebas de sistema.

export class ErrorApi extends Error {
  constructor(status, cuerpo) {
    super((cuerpo && cuerpo.mensaje) || `Error ${status}`);
    this.status = status;
    this.faltantes = (cuerpo && cuerpo.faltantes) || [];
  }
}

async function pedir(metodo, ruta, cuerpo) {
  const opciones = { method: metodo, headers: { Accept: 'application/json' } };
  if (cuerpo !== undefined) {
    opciones.headers['Content-Type'] = 'application/json';
    opciones.body = JSON.stringify(cuerpo);
  }
  const resp = await fetch(`/api${ruta}`, opciones);
  const texto = await resp.text();
  const datos = texto ? JSON.parse(texto) : null;
  if (!resp.ok) throw new ErrorApi(resp.status, datos);
  return datos;
}

const get = (ruta) => pedir('GET', ruta);
const post = (ruta, cuerpo) => pedir('POST', ruta, cuerpo);

export const api = {
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
