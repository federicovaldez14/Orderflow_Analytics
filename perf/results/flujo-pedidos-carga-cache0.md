# Resultado flujo-pedidos - escenario carga

Fecha: 2026-10-05T00:41:00.164Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 1064628 |
| Throughput | 4431.8 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 5.9 / 19.5 / 27.2 / 728.7 ms |
| Duración promedio de iteración | 49.2 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| crear_pedido | 31.1 | ≤ 500 ms → cumple |
| avanzar_pedido | 27.2 | ≤ 300 ms → cumple |
| dividir_cuenta | 22.0 | ≤ 300 ms → cumple |

## Umbrales (SLO)

- `checks` rate>0.99: cumple
- `http_req_failed` rate<0.01: cumple
- `http_req_duration` p(95)<500: cumple
- `http_req_duration{name:dividir_cuenta}` p(95)<300: cumple
- `http_req_duration{name:crear_pedido}` p(95)<500: cumple
- `http_req_duration{name:avanzar_pedido}` p(95)<300: cumple
