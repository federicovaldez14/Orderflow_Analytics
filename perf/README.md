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
ETIQUETA=volumen-medio CACHE=3 bash perf/run-perf.sh baseline
BD=archivo CACHE=3 bash perf/run-perf.sh baseline carga
```

Si el servicio se cae, el script lo detecta, muestra las últimas líneas del log y deja
`perf/results/CAIDA-<sufijo>.txt` con la causa (`OutOfMemoryError` o saturación). Con `-XX:+ExitOnOutOfMemoryError` una falta de memoria queda
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

**Archivos de resultados.** Cada ejecución deja en `results/`, con un sufijo por corrida (`-cache0`, `-cache3`,
`-cache3-archivo`, `-cache3-volumen-medio`…):

- `<script>-<escenario>-<sufijo>.md` – resumen: throughput, tasa de error, p50/p90/p95/máx, p95 por
  endpoint contra su SLO y el estado de cada umbral.
- `actuator-*-<sufijo>.json` – métricas del servidor (latencia por endpoint y conexiones del pool en espera).
- `pedidos-<sufijo>.txt` y `CAIDA-<sufijo>.txt` – pedidos en la base antes de medir la analítica y, si
  hubo caída, en qué paso y por qué.
- `<script>-<escenario>.json` (sin sufijo) – el resumen completo de k6 de la última corrida.

## 5. Hipótesis de cuello de botella (escritas antes de medir)

1. **Analítica O(n):** `ServicioAnalitica` lee *todos* los pedidos con sus líneas en cada consulta.
   Su costo debería crecer con el historial. Mitigación que permite la arquitectura: CQRS (módulo 5),
   un modelo de lectura con totales precalculados detrás del mismo puerto.
2. **Candados de fila en ingredientes populares:** muchas recetas usan AZÚCAR, PAPA o HUEVO; los
   pedidos que los descuentan a la vez se esperan unos a otros (la transacción es la que protege el
   stock). Se espera que `crear_pedido` sea el endpoint más lento del flujo.
3. **Pool de conexiones:** Tomcat atiende hasta 200 hilos y el pool Hikari tiene 20 conexiones;
   en estrés debería aparecer espera por conexión (`hikaricp.connections.pending`).

## 6. Resultados

Todas las corridas: portátil Windows 11, Java 17, `-Xmx1g`, k6 en la misma máquina. Cada archivo
`results/<script>-<escenario>-<sufijo>.md` tiene el detalle (p50/p90/p95/máx por endpoint y estado de
cada umbral); `CAIDA-<sufijo>.txt` registra cada caída con su causa.

### 6.1 Flujo de pedidos (R1 + R2 + concurrencia) — tipos **baseline** y **carga**

| Corrida | Escenario | Throughput | p95 total | p95 crear / avanzar / dividir | Errores | Checks | SLO |
|---|---|---|---|---|---|---|---|
| sin caché (`cache0`) | baseline, 10 VUs | 3.189 req/s | 6,6 ms | 8,5 / 6,0 / 5,7 ms | 0 % | 100 % | ✅ cumple |
| sin caché (`cache0`) | carga, 50 VUs | 4.432 req/s | 27,2 ms | 31,1 / 27,2 / 22,0 ms | 0 % | 100 % | ✅ cumple |
| con caché (`cache3`) | baseline, 10 VUs | 3.797 req/s | 5,7 ms | 7,5 / 5,1 / 5,0 ms | 0 % | 100 % | ✅ cumple |
| con caché (`cache3`) | carga, 50 VUs | 3.731 req/s | 29,3 ms | 45,6 / 23,6 / 20,8 ms | 0 % | 100 % | ✅ cumple |
| con caché, H2 en archivo | carga, 50 VUs | 4.645 req/s | 22,2 ms | 25,9 / 21,4 / 20,1 ms | 0 % | 100 % | ✅ cumple |

Al final de **cada** corrida del flujo, el `teardown` de k6 reportó *"20 ingredientes, 0 con stock
negativo"*: con más de un millón de peticiones concurrentes, la regla de R1 no se rompió. El check
"la cuenta dividida cuadra al peso" (R2) quedó en 100 %.

### 6.2 Analítica (R3) — antes y después de la mitigación

| Corrida | Pedidos en la base | Usuarios | Throughput | p95 | Errores | Resultado |
|---|---|---|---|---|---|---|
| Panel con el historial de demo (sin caché) | ≈ 150 | 5 | 2.558 req/s | 3,3 ms | 0 % | ✅ cumple |
| Panel con el historial de demo (con caché) | ≈ 150 | 5 | 4.736 req/s | 1,5 ms | 0 % | ✅ cumple |
| **Volumen medio, sin caché** | 47.576 | 30 | — | — | 99,96 % | ❌ **caída** (`OutOfMemoryError`) |
| **Volumen medio, con caché + candado único** | 47.096 | 30 | **6.035 req/s** | **7,7 ms** | **0 %** | ✅ **cumple** |
| Estrés de historial, sin caché | 251.624 | 30 | — | — | 100 % | ❌ caída |
| Estrés de historial, con caché | 225.330 | 30 | — | ≈ 4 s (7 respuestas) | 100 % | ❌ caída (`OutOfMemoryError`) |
| Estrés de historial, con caché y H2 en archivo | 264.618 | 30 | 5 req/s | 5.056 ms | 99,7 % | ❌ sin caída por memoria, pero saturado: cada cálculo ≈ 4,8 s |

*Volumen medio* = la base después del escenario baseline del flujo (≈ 47.000 pedidos, unos 8 meses
de un restaurante con 200 pedidos al día). *Estrés de historial* = después del escenario de carga
(≈ 225.000–265.000 pedidos, más de 3 años de operación). En las filas marcadas como caída, k6 muestra
p95 = 0 ms en el resumen: no es una respuesta rápida, son conexiones rechazadas por un servidor caído.

## 7. Análisis

**El cuello de botella es la analítica (hipótesis 1, confirmada).** Para calcular el panel,
`ServicioAnalitica` trae a memoria todos los pedidos del periodo con sus líneas y aplica las seis
estrategias de reporte. Es O(n) en memoria y en tiempo.

1. **Sin caché**, 30 usuarios disparan 30 cálculos simultáneos: 30 copias del historial en el heap.
   Con 47.000 pedidos ya se agota 1 GB y la JVM termina con `OutOfMemoryError`.
2. **Con caché de 3 s y un solo cálculo a la vez** (la mitigación), 29 de cada 30 consultas
   reciben el resultado ya calculado. Con el mismo volumen se pasó de **caída** a **p95 = 7,7 ms y
   0 % de errores**. La 2.ª corrida mostró que el candado tenía que ser **único** y no uno por
   periodo: con todos los pedidos creados el mismo día, HOY, SEMANA y TODO son el mismo historial,
   y tres cálculos simultáneos volvían a tumbar el servicio (`ServicioAnaliticaTest.shouldNeverComputeTwoPeriodsAtTheSameTime`).
3. **Con más de 200.000 pedidos, un solo cálculo ya no cabe.** H2 en memoria guarda todas las filas
   *en el mismo heap* que la aplicación, y un cálculo materializa otra copia completa. Cambiar solo la
   configuración del adaptador a **H2 en archivo** (`-BaseDatos archivo`) eliminó el
   `OutOfMemoryError`, pero el cálculo de 265.000 pedidos tarda ≈ 4,8 s: no cumple el SLO.

**Hipótesis 2 (candados de fila en el inventario):** confirmada parcialmente. `crear_pedido` es el
endpoint más lento del flujo en todas las corridas de carga (25,9–45,6 ms frente a 20–27 ms de los
demás), pero queda muy por debajo del SLO de 500 ms.

**Hipótesis 3 (pool de conexiones):** no se observó. `hikaricp.connections.pending` fue 0 al final de
cada escenario (`actuator-hikari-pending-*.json`; es una lectura puntual, no descarta esperas breves).
El flujo nunca superó 46 ms de p95 con 50 usuarios.

**Qué ofrece la arquitectura para mitigarlo:**

| Mitigación | Qué se tocó | Resultado |
|---|---|---|
| Filtro por fecha en SQL (HOY, SEMANA) | Puerto `PedidoRepositorio.listarCreadosDesde` + adaptador JDBC | Menos filas por consulta |
| Caché de 3 s con un solo cálculo a la vez | Solo el caso de uso `ServicioAnalitica` | Volumen medio: de caída a p95 7,7 ms |
| H2 en archivo | **Solo configuración** del adaptador (ADR-002) | Sin `OutOfMemoryError` a 265.000 pedidos |
| *Pendiente:* CQRS, un modelo de lectura con totales por hora, plato y categoría que se actualiza al cerrar cada pedido | Un puerto de lectura nuevo y su adaptador; el dominio y los controladores no cambian | Costo O(1) por consulta, independiente del historial |

Ninguna mitigación tocó el dominio ni los controladores: la hexagonal permitió atacar el problema en
el caso de uso y en el adaptador de persistencia. **Lo que no se resolvió:** la analítica sobre todo el
historial no escala a cientos de miles de pedidos; para eso falta el modelo de lectura (CQRS), que
queda como trabajo del Corte 3 ([arquitectura.md §7](../docs/arquitectura.md#7-límites-conocidos-y-trabajo-para-el-corte-3)).

**Trade-off de la caché:** el panel puede mostrar datos con hasta 3 s de antigüedad; la web y Swing
refrescan cada 2–2,5 s, así que en la práctica no se nota.

## 8. Historial de corridas

1. **28/09 — primera corrida** (código sin mitigación, sin límite de memoria): el servicio dejó de
   responder a los ~174 s del paso de analítica con decenas de miles de pedidos. Origen de la
   mitigación.
2. **28/09 — segunda corrida** (caché con un candado por periodo): flujo en verde, analítica con
   ~199.000 pedidos caída por `OutOfMemoryError`. Origen del candado único.
3. **04/10 — corridas finales** (código entregado): las de las tablas de la sección 6.
