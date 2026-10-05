# Resultado analitica - escenario carga

Fecha: 2026-10-05T01:04:44.979Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 912 |
| Throughput | 5.0 req/s |
| Tasa de error HTTP | 99.67 % |
| Latencia p50 / p90 / p95 / máx | 4999.5 / 5014.2 / 5055.8 / 5154.0 ms |
| Duración promedio de iteración | 5006.9 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 5055.8 | ≤ 500 ms → NO cumple |

## Umbrales (SLO)

- `http_req_duration{name:analitica_panel}` p(95)<500: NO cumple
- `http_req_failed` rate<0.01: NO cumple
- `checks` rate>0.99: NO cumple
