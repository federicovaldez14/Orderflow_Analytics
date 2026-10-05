# Resultado flujo-pedidos - escenario carga

Fecha: 2026-10-05T01:01:37.601Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 1115518 |
| Throughput | 4644.7 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 6.4 / 17.0 / 22.2 / 273.4 ms |
| Duración promedio de iteración | 47.0 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| crear_pedido | 25.9 | ≤ 500 ms → cumple |
| avanzar_pedido | 21.4 | ≤ 300 ms → cumple |
| dividir_cuenta | 20.1 | ≤ 300 ms → cumple |

## Umbrales (SLO)

- `http_req_duration` p(95)<500: cumple
- `http_req_duration{name:crear_pedido}` p(95)<500: cumple
- `http_req_failed` rate<0.01: cumple
- `checks` rate>0.99: cumple
- `http_req_duration{name:avanzar_pedido}` p(95)<300: cumple
- `http_req_duration{name:dividir_cuenta}` p(95)<300: cumple
