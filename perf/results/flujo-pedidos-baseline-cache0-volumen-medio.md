# Resultado flujo-pedidos - escenario baseline

Fecha: 2026-10-05T01:26:00.131Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 237023 |
| Throughput | 3925.3 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 1.6 / 3.7 / 5.4 / 173.6 ms |
| Duración promedio de iteración | 12.6 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| crear_pedido | 7.6 | ≤ 500 ms → cumple |
| avanzar_pedido | 4.9 | ≤ 300 ms → cumple |
| dividir_cuenta | 4.6 | ≤ 300 ms → cumple |

## Umbrales (SLO)

- `http_req_duration{name:dividir_cuenta}` p(95)<300: cumple
- `http_req_duration` p(95)<500: cumple
- `http_req_failed` rate<0.01: cumple
- `http_req_duration{name:crear_pedido}` p(95)<500: cumple
- `http_req_duration{name:avanzar_pedido}` p(95)<300: cumple
- `checks` rate>0.99: cumple
