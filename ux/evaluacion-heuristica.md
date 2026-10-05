# Evaluación heurística de usabilidad — POS web (Corte 2)

> Bonificación "Evaluación de UX". Método: **evaluación heurística de Nielsen** (10 heurísticas) con
> la **escala de severidad de Nielsen (0–4)**, recorriendo las tareas principales del mesero, la
> cocina y el administrador. Cada hallazgo tiene heurística, evidencia, severidad y decisión; los de
> severidad ≥ 2 se corrigieron y la mejora principal quedó **verificada con una prueba automatizada**.

| | |
|---|---|
| Sistema evaluado | POS web (`http://localhost:8080`), versión del commit de esta entrega |
| Evaluadores | Equipo del proyecto |
| Dispositivos | Escritorio 1440×900 (Edge) y celular 375×812 (modo responsivo) |
| Escala de severidad | 0 no es problema · 1 cosmético · 2 menor · 3 mayor · 4 catástrofe |

## 1. Tareas recorridas

| # | Tarea | Rol | Pasos (clics) |
|---|---|---|---|
| T1 | Tomar la comanda de una mesa (3 platos) y enviarla a cocina | Mesero | 5 |
| T2 | Llevar un pedido de *Creado* a *Entregado* | Cocina | 3 |
| T3 | Dividir la cuenta por consumo entre 2 personas con 10 % de propina | Mesero | 7 |
| T4 | Registrar un conteo físico de un ingrediente y ver su historial | Administrador | 4 |
| T5 | Consultar ventas y platos más pedidos de la semana | Dueño | 2 |

## 2. Hallazgos priorizados

| ID | Heurística | Hallazgo | Evidencia | Sev. | Decisión |
|---|---|---|---|---|---|
| **H5-1** | H5 Prevención de errores | Si el mesero está armando una comanda y toca otra mesa (lo llaman de otra mesa), **la comanda sin enviar se borra sin aviso**. | T1: con 2 platos agregados en la mesa 2, tocar la mesa 7 y volver → comanda vacía. | **3** | **Corregido** |
| **H3-1** | H3 Control y libertad | Quitar un plato de un pedido ya enviado es **un solo clic sin confirmación ni deshacer**; la "x" está junto al importe. | Panel de mesa ocupada, botón "x" de cada línea. | **2** | **Corregido** |
| **H5-2** | H5 Prevención de errores | En "Por porcentaje" se podía pulsar *Calcular* con porcentajes que no suman 100; el error llegaba después, desde el servidor. | T3 variante: 50 % + 40 % → aviso rojo del servidor. | **2** | **Corregido** |
| **H8-1** | H8 Diseño estético y minimalista | Los avisos flotantes aparecían abajo a la derecha y **tapaban los botones del panel** (Avanzar / Dividir / Cancelar) durante 3–6 s. | T1 y T2: tras "Enviar a cocina" el aviso cubría "Iniciar preparación". | **2** | **Corregido** |
| H2-1 | H2 Relación con el mundo real | En el historial de inventario, un *Ajuste por conteo* muestra la diferencia sin signo ("12"); no se sabe si sobró o faltó. | T4: conteo de 15 → 3 muestra "12 · queda 3". | 2 | Pendiente (requiere que la API exponga el stock anterior) |
| H1-1 | H1 Visibilidad del estado | La web se actualiza sola cada ~2,5 s, pero no indica cuándo fue la última actualización. | Indicador "En línea" sin hora. | 1 | Pendiente |
| H7-1 | H7 Flexibilidad y eficiencia | No hay atajos ni búsqueda en la carta; con 10 platos no estorba, con una carta grande sí. | T1 | 1 | Pendiente |
| H10-1 | H10 Ayuda y documentación | No hay ayuda en pantalla; los textos de estados vacíos orientan, pero un mesero nuevo no sabe qué es "Por consumo". | T3 | 1 | Pendiente |

Heurísticas sin hallazgos relevantes: H4 Consistencia (mismos colores y nombres de estado en web,
Swing y API), H6 Reconocer antes que recordar (los botones dicen la acción siguiente: "Iniciar
preparación", "Marcar como listo"…), H9 Recuperación de errores (los errores del dominio llegan con su
mensaje y, si falta stock, con la lista de ingredientes que faltan).

## 3. Métricas

| Métrica | Valor |
|---|---|
| Hallazgos totales | 8 |
| Por severidad (4 / 3 / 2 / 1) | 0 / 1 / 4 / 3 |
| Hallazgos de severidad ≥ 2 corregidos | **4 de 5 (80 %)** |
| Hallazgos de severidad ≥ 3 corregidos | **1 de 1 (100 %)** |
| Tareas completables sin ayuda (T1–T5) | 5 de 5 |
| Mejoras verificadas con prueba automatizada | 1 (H5-1, Selenium) + 3 verificadas manualmente |

## 4. Mejoras aplicadas y cómo se verificaron

| ID | Cambio | Dónde | Verificación |
|---|---|---|---|
| H5-1 | Las comandas sin enviar se guardan **por mesa** (borrador). La mesa muestra "Sin enviar · N" en el mapa hasta que se envía. | `src/main/resources/static/js/vistas/salon.js` (`borradores`, `platosSinEnviar`) | **Automática:** `PosWebUiIT.shouldKeepUnsentOrderWhenSwitchingTables` — arma la comanda de la mesa 2, cambia a la 7, comprueba el aviso "Sin enviar" en la mesa 2, vuelve y encuentra las 2 gaseosas. |
| H3-1 | Quitar un plato pide confirmación con un diálogo que explica qué pasa con el inventario. | `salon.js` (acción `data-quitar`) | Manual: el diálogo aparece; "Volver" no modifica el pedido. |
| H5-2 | El botón *Calcular* se deshabilita mientras los porcentajes no sumen 100, y el indicador lo dice. | `src/main/resources/static/js/division.js` (`pintarSumaPorcentajes`) | Manual: 50 + 40 → botón deshabilitado y "Suman 90 % (deben sumar 100)". |
| H8-1 | Los avisos se muestran arriba a la derecha en escritorio (en celular siguen abajo, sobre la barra de navegación). | `src/main/resources/static/css/app.css` (`.toasts`) | Manual: tras enviar la comanda, los botones del panel quedan libres. |
