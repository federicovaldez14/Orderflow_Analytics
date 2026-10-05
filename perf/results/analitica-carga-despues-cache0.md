# Resultado analitica - escenario carga

Fecha: 2026-10-05T00:44:03.255Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 636974 |
| Throughput | 3538.7 req/s |
| Tasa de error HTTP | 100.00 % |
| Latencia p50 / p90 / p95 / máx | 0.0 / 0.0 / 0.0 / 5165.5 ms |
| Duración promedio de iteración | 7.0 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 0.0 | ≤ 500 ms → cumple |

## Umbrales (SLO)

- `checks` rate>0.99: NO cumple
- `http_req_duration{name:analitica_panel}` p(95)<500: cumple
- `http_req_failed` rate<0.01: NO cumple
