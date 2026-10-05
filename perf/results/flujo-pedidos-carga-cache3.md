# Resultado flujo-pedidos - escenario carga

Fecha: 2026-10-05T00:50:39.423Z

| Métrica | Valor |
|---|---|
| Peticiones totales | 896768 |
| Throughput | 3731.4 req/s |
| Tasa de error HTTP | 0.00 % |
| Latencia p50 / p90 / p95 / máx | 8.7 / 22.0 / 29.3 / 841.2 ms |
| Duración promedio de iteración | 58.5 ms |

## Latencia por endpoint (p95)

| Endpoint | p95 (ms) | SLO |
|---|---|---|
| crear_pedido | 45.6 | ≤ 500 ms → cumple |
| avanzar_pedido | 23.6 | ≤ 300 ms → cumple |
| dividir_cuenta | 20.8 | ≤ 300 ms → cumple |

## Umbrales (SLO)

- `http_req_duration` p(95)<500: cumple
- `http_req_duration{name:avanzar_pedido}` p(95)<300: cumple
- `http_req_duration{name:crear_pedido}` p(95)<500: cumple
- `http_req_failed` rate<0.01: cumple
- `checks` rate>0.99: cumple
- `http_req_duration{name:dividir_cuenta}` p(95)<300: cumple
