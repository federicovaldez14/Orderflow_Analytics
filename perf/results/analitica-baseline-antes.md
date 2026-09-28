# Resultado analitica - escenario baseline

Fecha: 2026-09-28T16:56:27.655Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 183908 |
| Throughput | 3065.1 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 1.2 / 1.9 / 2.6 / 203.5 ms |
| Duración promedio de iteración | 1.6 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 2.6 | ≤ 500 ms → cumple |

## Umbrales (SLO)

- `http_req_duration{name:analitica_panel}` p(95)<500: cumple
- `http_req_failed` rate<0.01: cumple
- `checks` rate>0.99: cumple
