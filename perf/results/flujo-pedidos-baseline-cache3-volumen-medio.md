# Resultado flujo-pedidos - escenario baseline

Fecha: 2026-10-05T01:17:42.839Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 234623 |
| Throughput | 3892.9 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 1.6 / 3.8 / 5.5 / 158.6 ms |
| Duración promedio de iteración | 12.7 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| crear_pedido | 7.4 | ≤ 500 ms → cumple |
| avanzar_pedido | 5.0 | ≤ 300 ms → cumple |
| dividir_cuenta | 4.7 | ≤ 300 ms → cumple |

## Umbrales (SLO)

- `http_req_duration{name:dividir_cuenta}` p(95)<300: cumple
- `checks` rate>0.99: cumple
- `http_req_failed` rate<0.01: cumple
- `http_req_duration` p(95)<500: cumple
- `http_req_duration{name:avanzar_pedido}` p(95)<300: cumple
- `http_req_duration{name:crear_pedido}` p(95)<500: cumple
