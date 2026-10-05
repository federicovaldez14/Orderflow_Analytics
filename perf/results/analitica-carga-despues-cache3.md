# Resultado analitica - escenario carga

Fecha: 2026-10-05T00:53:41.835Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 463025 |
| Throughput | 2572.3 req/s |
| Tasa de error HTTP | 100.00 % |
| Latencia p50 / p90 / p95 / máx | 0.0 / 0.0 / 0.0 / 5333.0 ms |
| Duración promedio de iteración | 9.7 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 0.0 | ≤ 500 ms → cumple |

## Umbrales (SLO)

- `http_req_failed` rate<0.01: NO cumple
- `checks` rate>0.99: NO cumple
- `http_req_duration{name:analitica_panel}` p(95)<500: cumple
