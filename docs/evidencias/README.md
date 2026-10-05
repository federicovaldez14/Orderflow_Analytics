# Evidencia de ejecución

Generada con el código de esta entrega (04/10/2026).

| Archivo | Qué es | Cómo regenerarlo |
|---|---|---|
| [`resumen-pruebas.txt`](resumen-pruebas.txt) | Resultado de cada clase de prueba: 165 unitarias, 32 de integración y sistema, 4 de UI; todas en verde | `mvn clean verify -Pui-tests` (reportes en `target/surefire-reports/` y `target/failsafe-reports/`) |
| [`cobertura-jacoco.png`](cobertura-jacoco.png) | Captura del reporte de cobertura del núcleo (dominio + aplicación) | `mvn test` → `target/site/jacoco/index.html` |
| [`jacoco.csv`](jacoco.csv) | Cobertura por clase (instrucciones, ramas, líneas, métodos) | `mvn test` → `target/site/jacoco/jacoco.csv` |
| [`../../perf/results/`](../../perf/results/) | Resultados de cada corrida de carga (p95, throughput, errores, umbrales), métricas de Actuator y registro de caídas con su causa | `perf/run-perf.ps1` (ver [`perf/README.md`](../../perf/README.md)) |

El CI (`.github/workflows/ci.yml`) vuelve a generar los reportes de pruebas y la cobertura en cada
push y los publica como artefactos (`reportes-pruebas`, `reportes-pruebas-ui`).
