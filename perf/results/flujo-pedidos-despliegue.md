# Resultado flujo-pedidos - escenario despliegue

Fecha: 2026-11-15T17:17:06.170Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 12940 |
| Throughput | 275.6 req/s |
| Tasa de error HTTP | 0.04 % |
| Latencia p50 / p90 / p95 / máx | 12.8 / 33.0 / 41.6 / 134.5 ms |
| Duración promedio de iteración | 87.2 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| crear_pedido | 59.9 | ≤ 500 ms → cumple |
| avanzar_pedido | 23.9 | ≤ 300 ms → cumple |
| dividir_cuenta | 14.2 | ≤ 300 ms → cumple |

## Umbrales (SLO)

- `checks` rate>0.99: cumple
- `http_req_duration{name:dividir_cuenta}` p(95)<300: cumple
- `http_req_duration{name:crear_pedido}` p(95)<500: cumple
- `http_req_duration{name:avanzar_pedido}` p(95)<300: cumple
- `http_req_failed` rate<0.01: cumple
- `http_req_duration` p(95)<500: cumple
- `ingredientes_negativos` count==0: cumple
