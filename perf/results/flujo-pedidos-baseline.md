# Resultado flujo-pedidos - escenario baseline

Fecha: 2026-09-28T17:33:01.867Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 162408 |
| Throughput | 2686.8 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 2.2 / 6.3 / 9.1 / 136.7 ms |
| Duración promedio de iteración | 18.4 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| crear_pedido | 13.6 | ≤ 500 ms → cumple |
| avanzar_pedido | 7.9 | ≤ 300 ms → cumple |
| dividir_cuenta | 7.1 | ≤ 300 ms → cumple |

## Umbrales (SLO)

- `http_req_duration{name:crear_pedido}` p(95)<500: cumple
- `http_req_duration{name:avanzar_pedido}` p(95)<300: cumple
- `http_req_failed` rate<0.01: cumple
- `checks` rate>0.99: cumple
- `http_req_duration` p(95)<500: cumple
- `http_req_duration{name:dividir_cuenta}` p(95)<300: cumple
