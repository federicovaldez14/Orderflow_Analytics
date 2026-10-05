# Resultado flujo-pedidos - escenario baseline

Fecha: 2026-10-05T00:46:37.893Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 229048 |
| Throughput | 3796.9 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 1.6 / 4.0 / 5.7 / 213.0 ms |
| Duración promedio de iteración | 13.1 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| crear_pedido | 7.5 | ≤ 500 ms → cumple |
| avanzar_pedido | 5.1 | ≤ 300 ms → cumple |
| dividir_cuenta | 5.0 | ≤ 300 ms → cumple |

## Umbrales (SLO)

- `checks` rate>0.99: cumple
- `http_req_duration` p(95)<500: cumple
- `http_req_duration{name:avanzar_pedido}` p(95)<300: cumple
- `http_req_failed` rate<0.01: cumple
- `http_req_duration{name:dividir_cuenta}` p(95)<300: cumple
- `http_req_duration{name:crear_pedido}` p(95)<500: cumple
