# Resultado flujo-pedidos - escenario baseline

Fecha: 2026-10-05T00:36:58.739Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 192658 |
| Throughput | 3189.4 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 2.0 / 4.6 / 6.6 / 215.6 ms |
| Duración promedio de iteración | 15.5 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| crear_pedido | 8.5 | ≤ 500 ms → cumple |
| avanzar_pedido | 6.0 | ≤ 300 ms → cumple |
| dividir_cuenta | 5.7 | ≤ 300 ms → cumple |

## Umbrales (SLO)

- `http_req_duration{name:avanzar_pedido}` p(95)<300: cumple
- `http_req_duration` p(95)<500: cumple
- `http_req_failed` rate<0.01: cumple
- `checks` rate>0.99: cumple
- `http_req_duration{name:dividir_cuenta}` p(95)<300: cumple
- `http_req_duration{name:crear_pedido}` p(95)<500: cumple
