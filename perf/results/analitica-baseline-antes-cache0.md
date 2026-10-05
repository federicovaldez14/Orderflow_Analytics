# Resultado analitica - escenario baseline

Fecha: 2026-10-05T00:35:57.670Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 153455 |
| Throughput | 2557.5 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 1.3 / 2.4 / 3.3 / 143.9 ms |
| Duración promedio de iteración | 1.9 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 3.3 | ≤ 500 ms → cumple |

## Umbrales (SLO)

- `checks` rate>0.99: cumple
- `http_req_failed` rate<0.01: cumple
- `http_req_duration{name:analitica_panel}` p(95)<500: cumple
