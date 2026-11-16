# Orderflow Analytics — Sistema de pedidos para restaurante

[![Pipeline](https://github.com/federicovaldez14/Orderflow_Analytics/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/federicovaldez14/Orderflow_Analytics/actions/workflows/ci.yml)

**Sistema desplegado:** <https://orderflow-analytics-sha.onrender.com> (plan gratuito de Render: si estuvo inactivo, la primera carga tarda ≈ 1 min) · usuarios con rol (pida las credenciales de demostración al grupo)

**Versiones:** [v3.0.0 y v3.0.1](https://github.com/federicovaldez14/Orderflow_Analytics/releases) (tag, `.jar` e imagen `ghcr.io/federicovaldez14/orderflow-analytics:vX.Y.Z`; historial en [CHANGELOG.md](CHANGELOG.md))

Sistema de gestión de pedidos para un restaurante: mapa de mesas, flujo de comandas con cocina y
mesero, **inventario de ingredientes con trazabilidad**, **división de la cuenta** y **analítica con
gráficas**, con usuarios y roles.

- **Corte 1:** diseño de clases (SOLID y patrones).
- **Corte 2:** arquitectura hexagonal respaldada con pruebas.
- **Corte 3:** entrega continua y segura. Cada cambio pasa por un pipeline que prueba, verifica la
  arquitectura y la seguridad, y despliega en Render con PostgreSQL en Neon.

| Documento | Contenido |
|---|---|
| [Tabla de trazabilidad](#tabla-de-trazabilidad) | Reto → atributo → decisión → prueba → **etapa del pipeline** → **umbral** (en este README) |
| [docs/devops.md](docs/devops.md) | Pipeline: diagrama, etapas, umbrales y por qué; despliegue y **reversión** |
| [docs/seguridad.md](docs/seguridad.md) | Modelo de amenazas STRIDE, controles, umbrales y registro de hallazgos |
| [docs/implementacion.md](docs/implementacion.md) | **Guía de implementación y operación** para el restaurante |
| [docs/evolucion.md](docs/evolucion.md) | Los tres cortes y cómo se resolvieron los pendientes del Corte 2 |
| [docs/arquitectura.md](docs/arquitectura.md) | Documento de arquitectura: estilos, C4 (contexto, contenedores, componentes) y **vista de despliegue** |
| [docs/adr/](docs/adr/) | [ADR-001 Hexagonal](docs/adr/ADR-001-arquitectura-hexagonal.md) · [ADR-002 Persistencia](docs/adr/ADR-002-persistencia-jdbc-h2-pool.md) · [ADR-003 Inventario](docs/adr/ADR-003-consistencia-inventario.md) · [ADR-004 Interfaz web](docs/adr/ADR-004-interfaz-web-adaptador.md) · [ADR-005 Despliegue](docs/adr/ADR-005-despliegue-render-neon-ghcr.md) · [ADR-006 Seguridad del pipeline](docs/adr/ADR-006-seguridad-del-pipeline.md) |
| [docs/pruebas.md](docs/pruebas.md) | Estrategia de pruebas, clases de equivalencia, resultados y análisis |
| [docs/docker.md](docs/docker.md) | Contenerización: Dockerfile multi-stage, Compose con PostgreSQL y observabilidad |
| [perf/](perf/README.md) | Pruebas de carga: plan, SLO, scripts k6 (incluida la prueba de humo) |
| [CHANGELOG.md](CHANGELOG.md) | Cambios de cada versión ([Releases](https://github.com/federicovaldez14/Orderflow_Analytics/releases)) |
| [docs/corte1.md](docs/corte1.md) | Documento original del Corte 1 |

---

## Cómo ejecutar

### Con Docker Compose (recomendado: solo requiere Docker)

```bash
cp .env.example .env            # y cambiar las claves: openssl rand -base64 24
docker compose up -d --build    # aplicación + PostgreSQL 18
docker compose ps               # esperar "healthy" en db y app
```

Abrir <http://localhost:8080> e iniciar sesión con `ORDERFLOW_ADMIN_USUARIO` /
`ORDERFLOW_ADMIN_CLAVE` del `.env`. La base de datos solo es accesible dentro de la red de Compose
y la aplicación arranca cuando PostgreSQL está listo (`depends_on: service_healthy`).

Con métricas y el tablero del SLO en Grafana (bonificación de observabilidad):

```bash
docker compose --profile observabilidad up -d --build
```

Grafana queda en <http://localhost:3000> con el tablero *Orderflow · SLO de la API*; Prometheus en
<http://localhost:9090>.

```bash
docker compose down             # detener (los datos quedan en el volumen datos-db)
```

### Sin Docker (desarrollo)

Requisitos: Java 17+ y Maven 3.8+.

```bash
mvn spring-boot:run             # POS de escritorio + web en http://localhost:8080 (H2 en memoria)
```

El administrador inicial es `admin`. Si no se define `ORDERFLOW_ADMIN_CLAVE`, la clave se genera y
se muestra una vez en el log.

### Pruebas

```bash
mvn test                 # unitarias + 8 reglas de arquitectura (ArchUnit) + cobertura (umbral 80 %)
mvn verify               # + integración (H2 y PostgreSQL con Testcontainers: requiere Docker) y sistema por HTTP
mvn verify -Pui-tests    # + pruebas de UI con Selenium (Edge en Windows, Chrome en Linux/macOS)
k6 run -e ORDERFLOW_CLAVE=<clave-admin> perf/scripts/humo_k6.js   # prueba de humo contra http://localhost:8080
```

### Trabajo en equipo y pipeline

`main` está protegida: los cambios entran solo por pull request, con el pipeline en verde y la
aprobación de otro integrante.

```bash
git checkout main && git pull
git checkout -b mi-cambio
mvn verify
git push -u origin mi-cambio    # abrir el pull request hacia main en GitHub
```

| Workflow | Cuándo | Qué hace |
|---|---|---|
| [Pipeline](.github/workflows/ci.yml) | Cada pull request y cada push a `main` | 1 construcción · 2 unitarias + cobertura · 3 integración · 4 arquitectura · 5 UI · 6 seguridad (CodeQL, Trivy, Gitleaks) · 7 imagen + escaneo · 8 (solo `main`) despliegue en Render + prueba de humo + carga contra el SLO |
| [Versión](.github/workflows/release.yml) | `git tag vX.Y.Z && git push origin vX.Y.Z` | Imagen `:vX.Y.Z` en GHCR + GitHub Release con el `.jar` |
| [Reversión](.github/workflows/reversion.yml) | Manual (Actions → Reversión → Run workflow) | Vuelve a desplegar la imagen de una versión anterior y la verifica |
| [Carga larga](.github/workflows/carga-larga.yml) | Manual o los lunes | Carga larga (pedidos + analítica) contra producción |

Los reportes de cada ejecución (pruebas, cobertura, seguridad, carga) quedan como artefactos en la
pestaña Actions. Detalle de cada etapa, umbral y su justificación: [docs/devops.md](docs/devops.md).

### API REST (resumen)

Toda la API requiere sesión (login en `POST /api/sesion/login` con `usuario` y `clave`) o HTTP Basic.

| Método y ruta | Para qué | Roles |
|---|---|---|
| `GET /api/menu` · `GET /api/mesas` | Carta y estado de cada mesa | todos |
| `POST /api/pedidos` `{"mesa":3,"lineas":[{"plato":"Bandeja Paisa","cantidad":2}]}` | Tomar pedido (descuenta inventario) | ADMIN, MESERO |
| `POST /api/pedidos/{id}/avanzar` | Avanzar el pedido | ADMIN, MESERO, COCINA |
| `POST /api/pedidos/{id}/cancelar` · `/items` · `DELETE /api/pedidos/{id}/items/{linea}` | Cancelar y editar | ADMIN, MESERO |
| `POST /api/pedidos/{id}/division` `{"metodo":"POR_CONSUMO", ...}` | Dividir la cuenta (Reto 2) | ADMIN, MESERO |
| `GET /api/inventario` · `/alertas` · `/movimientos?ingrediente=HUEVO` | Inventario y trazabilidad (Reto 1) | todos (lectura) |
| `POST /api/inventario/{codigo}/reposicion` · `/ajuste` | Entradas y conteo físico | ADMIN |
| `GET /api/analitica?periodo=HOY\|SEMANA\|TODO` | Analítica (Reto 3) | ADMIN |
| `GET` · `POST /api/usuarios` | Usuarios y roles | ADMIN |
| `GET /actuator/health/readiness` · `/actuator/info` | Salud y versión desplegada | público |
| `GET /actuator/prometheus` | Métricas | ADMIN, MONITOR |

Códigos: 400 dato inválido · 401 sin sesión · 403 el rol no lo permite o falta el token CSRF ·
404 no existe · 409 el negocio no lo permite (mesa ocupada, stock insuficiente).

---

## Estilo arquitectónico

**Hexagonal (puertos y adaptadores) en un solo despliegue.** La decisión está en
[ADR-001](docs/adr/ADR-001-arquitectura-hexagonal.md) y la comparación con otros estilos en
[arquitectura.md §3](docs/arquitectura.md#3-comparación-de-estilos-y-decisión). Dónde corre en
producción: [vista de despliegue](docs/arquitectura.md#8-vista-de-despliegue-corte-3).

```
src/main/java/com/restaurant/
├── dominio/                           NÚCLEO — Java puro, sin frameworks
│   ├── modelo/  estado/  fabrica/     Pedido, State, Factory Method (Corte 1)
│   ├── inventario/                    Reto 1
│   ├── cuenta/                        Reto 2 (Strategy)
│   └── reportes/                      Reto 3 (Strategy)
├── aplicacion/
│   ├── casodeuso/                     GestorPedidos, ServicioInventario, ServicioCuenta, ServicioAnalitica
│   └── puerto/salida/                 PedidoRepositorio, MenuRepositorio, InventarioRepositorio, AlertaInventario
└── infraestructura/                   ADAPTADORES
    ├── rest/  ui/                     entrada: HTTP y Swing (la web está en resources/static)
    ├── seguridad/                     Spring Security: login, roles, CSRF, auditoría, usuarios
    ├── persistencia/                  salida: JDBC (PostgreSQL o H2) + HikariCP
    ├── notificacion/                  salida: cocina, mesero, alertas de stock
    └── config/                        raíz de composición y datos iniciales
```

[`ArquitecturaTest`](src/test/java/com/restaurant/arquitectura/ArquitecturaTest.java) verifica
8 reglas en cada pull request (etapa 4).

---

## Tabla de trazabilidad

La tabla del Corte 2, ampliada con la **etapa del pipeline** que vuelve a verificar cada reto y el
**umbral que detiene el pipeline**. Los nombres de las etapas corresponden a los checks de
[ci.yml](.github/workflows/ci.yml).

| Reto (Corte 1) | Atributo de calidad | Decisión arquitectónica | Prueba que lo evidencia | Etapa del pipeline | Umbral que detiene el pipeline |
|---|---|---|---|---|---|
| **R1. Inventario con trazabilidad de ingredientes** | Integridad de datos y trazabilidad bajo concurrencia | Puerto `InventarioRepositorio`; descuento atómico `UPDATE … WHERE stock >= ?` con orden fijo de bloqueo y `CHECK stock >= 0` ([ADR-003](docs/adr/ADR-003-consistencia-inventario.md)); un movimiento por cada cambio | Unitarias: `IngredienteTest`, `RecetaYConsumoTest`, `ServicioInventarioTest` · Integración: `InventarioRepositorioJdbcIT` y **`PostgresAdaptadoresIT`** (40 hilos, stock 10, en H2 **y PostgreSQL**) · Carga: `flujo_pedidos_k6.js` | 2 · unitarias → 3 · integración → 8 · despliegue (carga) | Una prueba falla (≠ 10 ventas o stock < 0) · cobertura del núcleo < 80 % · en carga: `ingredientes_negativos > 0` o p95 `crear_pedido` ≥ 500 ms |
| **R2. Dividir la cuenta al gusto** | Exactitud y modificabilidad (nuevas formas de dividir) | Strategy `EstrategiaDivision` + `Repartidor` por resto mayor con dinero en `long` | `RepartidorTest`, `DivisionCuentaTest`, **propiedades jqwik** (2 × 500 casos) · `ApiDivisionCuentaIT` · `PosWebUiIT` · prueba de humo ("cuenta cuadra al peso") | 2 · unitarias → 3 · integración → 5 · UI → 8 · despliegue (humo y carga) | La suma de las partes ≠ total en algún caso · falla el check de humo · p95 `dividir_cuenta` ≥ 300 ms |
| **R3. Analítica más completa y con gráficas** — rendimiento | Rendimiento de consultas | Reportes como datos (Strategy); filtro por fecha en SQL y caché de 3 s con un solo cálculo a la vez | `ServicioAnaliticaTest` (20 usuarios → 1 cálculo) · `ApiAnaliticaIT` · `analitica_k6.js` · humo ("analítica responde con los 6 reportes") | 2 · unitarias → 8 · despliegue (humo) → **Carga larga** (manual/semanal) | Más de un cálculo simultáneo · falla el check de humo · en carga larga: p95 ≥ 500 ms o errores ≥ 1 % |
| **R3. Analítica con gráficas** — usabilidad | Usabilidad (que el administrador entienda las gráficas) | Mismo caso de uso para tres adaptadores (gráficas Swing, SVG web y JSON) | **Sin verificación automática:** requiere evaluación con usuarios reales del restaurante; el pipeline solo comprueba que los datos de las gráficas existan y sean correctos (R3 rendimiento) | — | — |
| **R0. Límites del Corte 1: sin persistencia y un solo usuario** | Disponibilidad de los datos y rendimiento con varios meseros | Puertos de salida con adaptadores JDBC; **PostgreSQL** en producción ([ADR-005](docs/adr/ADR-005-despliegue-render-neon-ghcr.md)); tres adaptadores de entrada sobre los mismos casos de uso | `PedidoRepositorioJdbcIT`, `PostgresAdaptadoresIT` · `ApiPedidosIT` · `PosWebUiIT` · `flujo_pedidos_k6.js` (escenario `despliegue`) | 3 · integración → 5 · UI → 8 · despliegue (carga) | Falla una prueba · p95 ≥ 500 ms, errores ≥ 1 % o checks ≤ 99 % contra el entorno desplegado |
| **Modificabilidad de la hexagonal** (sostiene R1–R3) | Modificabilidad: cambiar la base o la interfaz sin tocar el negocio | El dominio y los casos de uso no conocen frameworks ni adaptadores ([ADR-001](docs/adr/ADR-001-arquitectura-hexagonal.md)) | `ArquitecturaTest` (ArchUnit, reglas R1–R7) | 4 · reglas de arquitectura | Una clase del dominio o de la aplicación importa Spring, JDBC, Swing, la base de datos o un paquete de adaptadores; un adaptador de entrada usa la persistencia; hay un ciclo |
| **Seguridad: solo el personal autorizado opera** (exigencia del Corte 3 al publicar el sistema) | Seguridad: autenticación y autorización | Spring Security con roles, CSRF y auditoría ([seguridad.md](docs/seguridad.md)) | `SeguridadIT` (401/403/CSRF) · `FiltroAuditoriaTest` · `PosWebUiIT` flujo 5 (cada rol ve sus pantallas) · humo (inicia sesión) | 3 · integración → 6a/6b/6c · seguridad → 7 · imagen | Un rol accede a lo que no debe · hallazgo SAST ≥ 7.0 · CVE alta o crítica con corrección · un secreto en el historial · CVE crítica en la imagen |

Resultados detallados de las pruebas del Corte 2 (carga, concurrencia, cobertura):
[docs/pruebas.md](docs/pruebas.md) y [perf/README.md](perf/README.md).

**Lo que no se resolvió por completo** (detalle en [evolucion.md §4](docs/evolucion.md#4-pendientes-del-corte-2-y-cómo-se-resolvieron)):

- La analítica recalcula sobre el historial (cumple el SLO con ≈ 47.000 pedidos, no con más de
  200.000).
- Pedido e inventario se coordinan con una compensación, no con una transacción única.
- Los candados por mesa solo protegen dentro de una instancia.
- Dos CVE de Spring 6.2 están aceptadas con vencimiento.

---

## Bonificaciones

| Bonificación | Qué se entrega | Cómo verlo |
|---|---|---|
| **Observabilidad (Corte 3)** | La aplicación expone métricas Prometheus (`/actuator/prometheus`, protegido con un usuario `MONITOR`). Prometheus las recolecta y Grafana trae un tablero provisionado con las métricas del SLO: **latencia p95** contra la línea de 500 ms, **tasa de error 5xx** contra el 1 %, **throughput** por endpoint, conexiones a la base y memoria | `docker compose --profile observabilidad up -d` → <http://localhost:3000> · [observabilidad/](observabilidad/) · [captura del tablero](docs/evidencias/observabilidad/grafana-slo.png) |
| **Pruebas de UI (Corte 2)** | 5 pruebas con Selenium sobre el POS web (Page Object Model), incluido el inicio de sesión por rol; corren en la etapa 5 del pipeline | [`ui-tests/README.md`](ui-tests/README.md) · `mvn verify -Pui-tests` |

---

## Créditos y roles

| Integrante | Rol / contribución principal |
|---|---|
| Federico Valdez | Programador |
| Daniel Sanabria | Arquitecto |

Videos del Corte 1: [videos/](videos/).
