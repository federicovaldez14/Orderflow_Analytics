# Resultado analitica - escenario baseline

Fecha: 2026-09-28T17:32:00.737Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 214620 |
| Throughput | 3576.9 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 1.0 / 1.5 / 2.1 / 135.3 ms |
| Duración promedio de iteración | 1.4 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 2.1 | ≤ 500 ms → cumple |

## Umbrales (SLO)

- `http_req_failed` rate<0.01: cumple
- `checks` rate>0.99: cumple
- `http_req_duration{name:analitica_panel}` p(95)<500: cumple
