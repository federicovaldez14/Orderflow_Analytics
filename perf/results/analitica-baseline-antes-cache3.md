# Resultado analitica - escenario baseline

Fecha: 2026-10-05T00:45:37.114Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 284161 |
| Throughput | 4736.0 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 0.8 / 1.2 / 1.5 / 93.0 ms |
| Duración promedio de iteración | 1.0 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 1.5 | ≤ 500 ms → cumple |

## Umbrales (SLO)

- `http_req_duration{name:analitica_panel}` p(95)<500: cumple
- `http_req_failed` rate<0.01: cumple
- `checks` rate>0.99: cumple
