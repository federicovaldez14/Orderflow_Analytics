# ADR-003 — Consistencia del inventario con pedidos concurrentes

- **Estado:** aceptada
- **Fecha:** 2026-09-28

## Contexto

R1 exige no vender lo que no hay y dejar trazado cada cambio. Con varios meseros (o cientos de
peticiones en la prueba de carga) dos pedidos pueden pedir el último huevo al mismo tiempo. Un
esquema "leer stock → comprobar en Java → escribir" tiene una condición de carrera: los dos leen 1,
los dos comprueban que alcanza y los dos descuentan.

Además, un pedido usa varios ingredientes: si alcanza el arroz pero no el huevo, no debe descontarse
nada.

## Opciones consideradas

1. `synchronized` / candado en Java alrededor del descuento.
2. **Transacción en la base de datos con `UPDATE ... SET stock = stock - ? WHERE codigo = ? AND stock >= ?`**
   por ingrediente, todo el pedido en una transacción, filas en orden fijo, y `CHECK (stock >= 0)`.
3. Reservas asíncronas por eventos (consistencia eventual).

## Decisión

Opción 2, implementada en `InventarioRepositorioJdbc.descontar`:

1. Todo el consumo del pedido en **una transacción**.
2. Cada ingrediente con un **UPDATE condicional**: la base bloquea la fila y solo resta si alcanza;
   0 filas actualizadas = no alcanzó → *rollback* de todo y `StockInsuficienteException` con **todos**
   los faltantes.
3. Las filas se actualizan en **orden alfabético de código** (`CalculadoraConsumo` devuelve un
   `TreeMap`) para evitar interbloqueos entre dos pedidos que piden los mismos ingredientes en
   distinto orden.
4. `CHECK (stock >= 0)` en la tabla como segunda barrera.
5. En la misma transacción se inserta un `movimiento_inventario` por ingrediente con el stock
   resultante (trazabilidad).

La regla de negocio ("no se vende sin stock", "cancelar en Creado devuelve, en preparación es
merma") vive en el dominio (`Ingrediente`, `PoliticaDevolucion`); el adaptador aporta la
**atomicidad**, que solo la base de datos puede dar entre hilos y procesos.

## Consecuencias

**Positivas**
- Correcto aunque haya varias instancias de la aplicación sobre la misma base (a diferencia de un
  candado en Java).
- Probado: `InventarioRepositorioJdbcIT.shouldNeverOversellUnderConcurrency` lanza 40 hilos por 1
  huevo cada uno con 10 en stock y exige exactamente 10 ventas, 30 rechazos, stock 0 y 10
  movimientos.
- El historial es consistente con el stock porque se escribe en la misma transacción.

**Negativas**
- Los pedidos que comparten ingredientes populares se serializan en esas filas: bajo carga, esperan
  el candado (hipótesis 2 de la prueba de carga). Se configuró `LOCK_TIMEOUT=10000` en H2.
- La lógica de atomicidad queda en el adaptador (SQL), no en el dominio; un adaptador nuevo
  (PostgreSQL, otro motor) debe respetar el mismo contrato del puerto (documentado en
  `InventarioRepositorio`).
