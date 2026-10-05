# Resultado analitica - escenario baseline

Fecha: 2026-10-05T01:16:42.065Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 252986 |
| Throughput | 4216.4 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 0.9 / 1.3 / 1.6 / 106.1 ms |
| Duración promedio de iteración | 1.2 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 1.6 | ≤ 500 ms → cumple |

## Umbrales (SLO)

- `checks` rate>0.99: cumple
- `http_req_duration{name:analitica_panel}` p(95)<500: cumple
- `http_req_failed` rate<0.01: cumple
