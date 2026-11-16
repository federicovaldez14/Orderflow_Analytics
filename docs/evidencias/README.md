# Evidencia de ejecución

Generada con el código de esta entrega (04/10/2026).

| Archivo | Qué es | Cómo regenerarlo |
|---|---|---|
| [`resumen-pruebas.txt`](resumen-pruebas.txt) | Resultado de cada clase de prueba: 165 unitarias, 32 de integración y sistema, 4 de UI; todas en verde | `mvn clean verify -Pui-tests` (reportes en `target/surefire-reports/` y `target/failsafe-reports/`) |
| [`cobertura-jacoco.png`](cobertura-jacoco.png) | Captura del reporte de cobertura del núcleo (dominio + aplicación) | `mvn test` → `target/site/jacoco/index.html` |
| [`jacoco.csv`](jacoco.csv) | Cobertura por clase (instrucciones, ramas, líneas, métodos) | `mvn test` → `target/site/jacoco/jacoco.csv` |
| [`docker/`](docker/) | Contenedor `healthy` (`docker compose ps`, readiness, usuario sin privilegios) y POS web servido desde el contenedor | `docker compose up -d --build` (ver [`../docker.md`](../docker.md)) |
| [`observabilidad/grafana-slo.png`](observabilidad/grafana-slo.png) | **Corte 3, bonificación de observabilidad.** Tablero «Orderflow · SLO de la API» durante dos corridas del escenario `despliegue` de k6 (versión anterior del escenario: 5 usuarios, 45 s; 0 % de errores): p95 ≈ 33 ms contra el SLO de 500 ms, errores 5xx 0 %, throughput, p95 por endpoint, memoria y pool de conexiones (15/11/2026) | `docker compose --profile observabilidad up -d --build`, correr `flujo_pedidos_k6.js` con `SCENARIO=despliegue` y abrir <http://localhost:3000> |
| [`observabilidad/prometheus-targets.png`](observabilidad/prometheus-targets.png) | Prometheus recolectando `/actuator/prometheus` de la aplicación (objetivo `UP`, autenticado con el usuario de rol `MONITOR`) | <http://localhost:9090/targets> con el perfil `observabilidad` |
| [`../../perf/results/`](../../perf/results/) | Resultados de cada corrida de carga (p95, throughput, errores, umbrales), métricas de Actuator y registro de caídas con su causa | `perf/run-perf.ps1` (ver [`perf/README.md`](../../perf/README.md)) |

El CI (`.github/workflows/ci.yml`) vuelve a generar los reportes de pruebas y la cobertura en cada
push y los publica como artefactos (`reportes-pruebas`, `reportes-pruebas-ui`).
