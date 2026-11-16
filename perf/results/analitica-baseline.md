# Resultado analitica - escenario baseline

Fecha: 2026-11-15T17:17:34.608Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 11643 |
| Throughput | 774.0 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 2.9 / 4.3 / 5.4 / 192.5 ms |
| Duración promedio de iteración | 3.8 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 5.4 | ≤ 500 ms → cumple |

## Umbrales (SLO)

- `http_req_duration{name:analitica_panel}` p(95)<500: cumple
- `http_req_failed` rate<0.01: cumple
- `checks` rate>0.99: cumple
