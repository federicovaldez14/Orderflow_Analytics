# Resultado flujo-pedidos - escenario carga

Fecha: 2026-09-28T17:37:03.467Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 832828 |
| Throughput | 3467.5 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 9.3 / 23.6 / 31.5 / 380.9 ms |
| Duración promedio de iteración | 63.0 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| crear_pedido | 49.6 | ≤ 500 ms → cumple |
| avanzar_pedido | 24.9 | ≤ 300 ms → cumple |
| dividir_cuenta | 22.0 | ≤ 300 ms → cumple |

## Umbrales (SLO)

- `http_req_duration` p(95)<500: cumple
- `http_req_duration{name:crear_pedido}` p(95)<500: cumple
- `http_req_duration{name:avanzar_pedido}` p(95)<300: cumple
- `http_req_failed` rate<0.01: cumple
- `http_req_duration{name:dividir_cuenta}` p(95)<300: cumple
- `checks` rate>0.99: cumple
