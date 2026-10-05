# Resultado analitica - escenario carga

Fecha: 2026-10-05T01:20:44.742Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 1086355 |
| Throughput | 6035.3 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 2.1 / 6.0 / 7.7 / 293.0 ms |
| Duración promedio de iteración | 4.1 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 7.7 | ≤ 500 ms → cumple |

## Umbrales (SLO)

- `http_req_duration{name:analitica_panel}` p(95)<500: cumple
- `checks` rate>0.99: cumple
- `http_req_failed` rate<0.01: cumple
