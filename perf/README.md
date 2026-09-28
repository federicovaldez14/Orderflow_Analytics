# Pruebas de carga (k6)

Esta carpeta contiene el **plan**, los **scripts versionados** y los **resultados** de las pruebas
de carga del Corte 2. Los SLO se fijaron **antes** de ejecutar y están escritos como `thresholds`
dentro de cada script, así que k6 marca solo si se cumplen o no.

## 1. Plan: escenarios ligados a los retos

| Script | Reto / atributo de calidad | Qué hace cada usuario virtual |
|---|---|---|
| `scripts/flujo_pedidos_k6.js` | Reto 1 (inventario) + Reto 2 (dividir cuenta) + concurrencia de meseros · **Rendimiento, consistencia bajo concurrencia** | Toma un pedido de 1–3 platos en su mesa (descuenta inventario), lo avanza hasta *Entregado* y divide la cuenta con 10 % de propina. Al final verifica que ningún ingrediente quedó negativo. |
| `scripts/analitica_k6.js` | Reto 3 (analítica) · **Rendimiento / escalabilidad de consultas** | Consulta el panel `GET /api/analitica` (Hoy / Semana / Todo). Se ejecuta antes y después de cargar miles de pedidos para ver cómo crece el costo. |

## 2. SLO (definidos antes de ejecutar)

| Indicador | Objetivo | Por qué ese valor |
|---|---|---|
| p95 de toda petición del flujo | ≤ 500 ms | Un mesero no debe notar espera al confirmar en el POS. |
| p95 `crear_pedido` | ≤ 500 ms | Es la operación más cara: transacción de pedido + inventario. |
| p95 `avanzar_pedido`, `dividir_cuenta` | ≤ 300 ms | Operaciones sobre un solo pedido, sin inventario. |
| p95 `analitica_panel` | ≤ 500 ms | El panel se refresca cada 2 s en la UI. |
| Tasa de error HTTP | < 1 % | Mismo criterio del taller de carga. |
| Checks de negocio | > 99 % | 201 al crear, estado *Entregado*, la cuenta cuadra al peso. |

## 3. Tipos de prueba

| `SCENARIO` | flujo_pedidos | analitica |
|---|---|---|
| `baseline` | 10 VUs constantes, 1 min | 5 VUs, 1 min |
| `carga` | rampa 0→50 VUs, 3 min sostenido | rampa 0→30 VUs, 2 min sostenido |
| `estres` | 50→200 VUs en 2 min, 1 min sostenido | – |
| `pico` | 10→150 VUs en 15 s, 30 s, vuelta a 10 | – |

## 4. Cómo ejecutar

Requisitos: Java 17+, Maven, [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/)
(Windows: `winget install k6 --source winget`).

**Todo automático** (compila, levanta el servicio sin ventana con `-Xmx1g`, corre los escenarios,
verifica entre pasos que el servicio siga vivo, guarda métricas de Actuator y apaga):

```powershell
# "Antes": analítica sin caché (reproduce el cuello de botella)
powershell -ExecutionPolicy Bypass -File perf\run-perf.ps1 -CacheAnalitica 0
# "Después": con la mitigación (caché de 3 s, un solo cálculo a la vez)
powershell -ExecutionPolicy Bypass -File perf\run-perf.ps1 -CacheAnalitica 3
# Más tipos de prueba
powershell -ExecutionPolicy Bypass -File perf\run-perf.ps1 -Escenarios baseline,carga,estres,pico
```
```bash
CACHE=0 bash perf/run-perf.sh baseline carga
CACHE=3 bash perf/run-perf.sh baseline carga
```

Si el servicio se cae, el script lo detecta, muestra las últimas líneas del log y deja
`perf/results/CAIDA-cacheN.txt`. Con `-XX:+ExitOnOutOfMemoryError` una falta de memoria queda
explícita en `app-err.log` en vez de dejar el proceso en un estado indefinido.

**Manual:**

```bash
mvn -DskipTests package
java -jar target/orderflow-analytics-2.0.0.jar --orderflow.ui.enabled=false --orderflow.mesas=1000
# en otra terminal, desde la raíz del repo:
k6 run -e SCENARIO=baseline perf/scripts/flujo_pedidos_k6.js
k6 run -e SCENARIO=carga    perf/scripts/flujo_pedidos_k6.js
k6 run -e SCENARIO=carga    perf/scripts/analitica_k6.js
```

`--orderflow.mesas=1000` hace falta porque cada VU atiende su propia mesa (regla de negocio:
una comanda activa por mesa).

## 5. Resultados

Cada ejecución deja en `results/` (con sufijo `-cache0` o `-cache3` para comparar antes y después):

- `<script>-<escenario>.md` – resumen listo para pegar: throughput, tasa de error, p50/p90/p95/máx,
  p95 por endpoint contra su SLO y el estado de cada umbral.
- `<script>-<escenario>.json` – el resumen completo de k6.
- `actuator-*.json` – métricas del servidor (latencia por endpoint y conexiones del pool en espera).

El análisis de los resultados está en [`../docs/pruebas.md`](../docs/pruebas.md#4-pruebas-de-carga).

## 6. Hipótesis de cuello de botella (escritas antes de medir)

1. **Analítica O(n):** `ServicioAnalitica` lee *todos* los pedidos con sus líneas en cada consulta.
   Su p95 debería crecer con el historial. Mitigación que permite la arquitectura: CQRS (módulo 5),
   un modelo de lectura con totales precalculados detrás del mismo puerto.
2. **Candados de fila en ingredientes populares:** muchas recetas usan AZÚCAR, PAPA o HUEVO; los
   pedidos que los descuentan a la vez se esperan unos a otros (la transacción es la que protege el
   stock). Se espera que `crear_pedido` sea el endpoint más lento del flujo.
3. **Pool de conexiones:** Tomcat atiende hasta 200 hilos y el pool Hikari tiene 20 conexiones;
   en estrés debería aparecer espera por conexión (`hikaricp.connections.pending`).

## 7. Hallazgo de la primera ejecución

En la primera corrida completa (28/09/2026, portátil Windows, sin límite de memoria explícito y
**sin caché** en la analítica), los pasos de analítica *antes* y de flujo de pedidos terminaron,
pero en el paso 5 —30 usuarios consultando la analítica después de que la prueba de flujo había
creado decenas de miles de pedidos— **el servicio dejó de responder a los ~174 s** (k6 reportó
`connection refused` y superó los umbrales `checks` y `http_req_failed`).

Coincide con la **hipótesis 1** (causa probable; se confirma si `app-err.log` muestra `OutOfMemoryError`): cada consulta del panel traía a memoria todo el historial de
pedidos con sus líneas, y 30 consultas simultáneas multiplicaban esa memoria.

**Mitigación aplicada** (en `ServicioAnalitica` y el adaptador JDBC, sin tocar el dominio):

1. HOY y SEMANA se filtran en SQL (`PedidoRepositorio.listarCreadosDesde`, con índice por fecha).
2. Caché por periodo con vigencia configurable (`orderflow.analitica.cache-segundos`, 3 s por
   defecto) y **un solo cálculo a la vez**: los demás usuarios esperan y reciben el mismo
   resultado. Lo prueban `ServicioAnaliticaTest.shouldComputeOnlyOnceUnderConcurrentRequests` (20
   hilos, 1 lectura) y las pruebas de vigencia.

**Trade-off:** el panel puede mostrar datos con hasta 3 s de antigüedad; la UI ya refresca cada 2 s,
así que en la práctica no se nota. La solución de fondo sigue siendo CQRS (totales precalculados).
