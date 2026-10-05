# Resultado analitica - escenario baseline

Fecha: 2026-10-05T01:24:59.203Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 184384 |
| Throughput | 3073.0 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 1.2 / 2.0 / 2.7 / 98.3 ms |
| Duración promedio de iteración | 1.6 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 2.7 | ≤ 500 ms → cumple |

## Umbrales (SLO)

- `http_req_failed` rate<0.01: cumple
- `checks` rate>0.99: cumple
- `http_req_duration{name:analitica_panel}` p(95)<500: cumple
