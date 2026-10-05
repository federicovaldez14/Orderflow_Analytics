# Resultado analitica - escenario carga

Fecha: 2026-10-05T01:29:00.940Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 486966 |
| Throughput | 2705.4 req/s |
| Tasa de error HTTP | 99.96 % |
| Latencia p50 / p90 / p95 / máx | 0.0 / 0.0 / 0.0 / 5604.8 ms |
| Duración promedio de iteración | 9.2 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| analitica_panel | 0.0 | ≤ 500 ms → cumple |

## Umbrales (SLO)

- `checks` rate>0.99: NO cumple
- `http_req_failed` rate<0.01: NO cumple
- `http_req_duration{name:analitica_panel}` p(95)<500: cumple
