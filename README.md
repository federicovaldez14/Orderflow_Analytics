# Orderflow Analytics — Sistema de pedidos para restaurante (Corte 2)

Sistema de gestión de pedidos para un restaurante: mapa de mesas, flujo de comandas con cocina y
mesero, **inventario de ingredientes con trazabilidad**, **división de la cuenta** y **analítica con
gráficas**. Corte 2 de Diseño y Arquitectura de Software: el sistema del Corte 1 se reorganizó en
**arquitectura hexagonal** y se respaldó con pruebas unitarias, de integración y de carga.

| Documento | Contenido |
|---|---|
| [docs/arquitectura.md](docs/arquitectura.md) | Descripción, retos, comparación de estilos, diagramas C4 (contexto, contenedores, componentes), límites |
| [docs/adr/](docs/adr/) | [ADR-001 Hexagonal](docs/adr/ADR-001-arquitectura-hexagonal.md) · [ADR-002 Persistencia](docs/adr/ADR-002-persistencia-jdbc-h2-pool.md) · [ADR-003 Consistencia del inventario](docs/adr/ADR-003-consistencia-inventario.md) |
| [docs/pruebas.md](docs/pruebas.md) | Estrategia de pruebas, clases de equivalencia, resultados y análisis |
| [perf/README.md](perf/README.md) | Plan de carga: SLO, escenarios, scripts k6 |
| [docs/diagramas/](docs/diagramas/) | Diagrama de clases del núcleo y secuencia "tomar pedido con inventario" |
| [docs/corte1.md](docs/corte1.md) | Documento original del Corte 1 (SOLID y patrones) |

---

## Cómo ejecutar

**Requisitos:** Java 17 o superior y Maven 3.8+. Para carga: [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/)
(Windows: `winget install k6 --source winget`).

```bash
# 1. Aplicación: abre el POS de escritorio y levanta la web y la API en http://localhost:8080
mvn spring-boot:run

# o como jar
mvn -DskipTests package
java -jar target/orderflow-analytics-2.0.0.jar

# solo la web y la API, sin ventana (servidores, pruebas de carga)
java -jar target/orderflow-analytics-2.0.0.jar --orderflow.ui.enabled=false
```

**Versión web:** con la aplicación corriendo, abre <http://localhost:8080> en el navegador (también
desde una tablet o celular en la misma red, con la IP del computador). Tiene salón, cocina,
inventario, analítica y actividad, y se actualiza sola cada pocos segundos. Es un tercer adaptador de
entrada (HTML/CSS/JS en `src/main/resources/static/`) que usa la misma API REST: no toca el dominio,
y lo que se haga en la web aparece en la ventana de escritorio y al revés.

Al arrancar se cargan la carta (10 platos), 20 ingredientes con sus recetas y ~7 días de pedidos
históricos para la analítica (`orderflow.demo.historico=true`, se puede apagar).

```bash
# 2. Pruebas
mvn test      # unitarias + regla de arquitectura + cobertura (target/site/jacoco/index.html)
mvn verify    # + integración con H2 y pruebas de sistema por HTTP (*IT)

# 3. Carga (compila, levanta el servicio, corre k6 y apaga)
powershell -ExecutionPolicy Bypass -File perf\run-perf.ps1      # Windows
bash perf/run-perf.sh baseline carga                            # Linux / macOS / Git Bash
```

### Trabajo en equipo

```bash
git clone https://github.com/federicovaldez14/Orderflow_Analytics.git
cd Orderflow_Analytics
git pull                      # main al día antes de empezar
git checkout -b mi-cambio     # una rama por tarea, creada desde main
mvn verify                    # antes de subir: todo en verde
git push -u origin mi-cambio  # luego pull request hacia main en GitHub
```

- La CI (`.github/workflows`) corre `mvn verify` en cada push y pull request.
- No se versionan `target/`, los logs de `perf/results/` ni la base H2 local (`data/`).
- Pendiente conocido: con ~200.000 pedidos la analítica bajo carga se sigue cayendo aun con caché
  (`perf/results/CAIDA-cache3.txt`); el candado de `ServicioAnalitica` es por periodo, así que
  pueden correr 3 cálculos a la vez.

### API REST (resumen)

| Método y ruta | Para qué |
|---|---|
| `GET /api/menu` · `GET /api/mesas` | Carta y estado de cada mesa |
| `POST /api/pedidos` `{"mesa":3,"lineas":[{"plato":"Bandeja Paisa","cantidad":2}]}` | Tomar pedido (descuenta inventario) |
| `POST /api/pedidos/{id}/avanzar` · `/cancelar` · `/items` · `DELETE /api/pedidos/{id}/items/{linea}` | Flujo y edición |
| `POST /api/pedidos/{id}/division` `{"metodo":"POR_CONSUMO", ...}` | Dividir la cuenta (Reto 2) |
| `GET /api/inventario` · `/alertas` · `/movimientos?ingrediente=HUEVO` · `GET /api/pedidos/{id}/inventario` | Inventario y trazabilidad (Reto 1) |
| `POST /api/inventario/{codigo}/reposicion` · `/ajuste` | Entradas y conteo físico |
| `GET /api/analitica?periodo=HOY\|SEMANA\|TODO` · `GET /api/analitica/reportes/{id}` | Analítica (Reto 3) |
| `GET /api/notificaciones` | Últimos avisos de cocina, mesero y stock bajo (los muestra la web) |
| `GET /actuator/health` · `/actuator/prometheus` | Salud y métricas |

Códigos: 400 dato inválido · 404 no existe · 409 el negocio no lo permite (mesa ocupada, stock
insuficiente con el detalle de faltantes, pedido cancelado).

---

## Estilo arquitectónico

**Hexagonal (puertos y adaptadores) en un solo despliegue** — ver
[ADR-001](docs/adr/ADR-001-arquitectura-hexagonal.md) y la comparación contra capas, microservicios
y eventos en [arquitectura.md §3](docs/arquitectura.md#3-comparación-de-estilos-y-decisión).

```
src/main/java/com/restaurant/
├── OrderflowApplication.java          arranque (Spring Boot)
├── dominio/                           NÚCLEO — Java puro, sin frameworks
│   ├── modelo/  estado/  fabrica/     Pedido, State, Factory Method (Corte 1)
│   ├── observador/Notificador         puerto de notificaciones (Observer)
│   ├── inventario/                    Reto 1: Ingrediente, Receta, CalculadoraConsumo, PoliticaDevolucion
│   ├── cuenta/                        Reto 2: EstrategiaDivision (Strategy), DivisorCuenta, Repartidor
│   └── reportes/                      Reto 3: EstrategiaReporte (Strategy), Reporte, Indicadores, Periodo
├── aplicacion/
│   ├── casodeuso/                     GestorPedidos, ServicioInventario, ServicioCuenta, ServicioAnalitica
│   └── puerto/salida/                 PedidoRepositorio, MenuRepositorio, InventarioRepositorio, AlertaInventario
└── infraestructura/                   ADAPTADORES
    ├── rest/                          entrada HTTP (controladores, DTOs, manejo de errores)
    ├── ui/                            entrada Swing (POS: mesas, pedidos, inventario, analítica, dividir cuenta)
    ├── persistencia/                  salida JDBC + H2 + HikariCP
    ├── notificacion/                  salida: cocina, mesero, alertas de stock
    └── config/                        composition root y datos iniciales

src/main/resources/static/             entrada WEB (POS en el navegador; consume /api)
├── index.html  css/app.css            estructura y tema oscuro
└── js/                                api.js (cliente REST), vistas/ (salón, cocina, inventario, analítica, actividad)
```

La regla "el dominio no depende de la infraestructura" la verifica
`ReglasDeDependenciaTest` en cada `mvn test`.

---

## Tabla de trazabilidad

| Reto (Corte 1) | Atributo de calidad | Decisión arquitectónica | Dónde está | Prueba que lo evidencia | Resultado |
|---|---|---|---|---|---|
| **R1. Inventario con trazabilidad de ingredientes** *(funcionalidad nueva exigida por el reto)* | Integridad de datos, trazabilidad (auditabilidad), confiabilidad bajo concurrencia | Componente de inventario detrás del puerto `InventarioRepositorio`; descuento atómico por pedido con `UPDATE ... WHERE stock >= ?`, orden fijo de bloqueo y `CHECK stock >= 0` ([ADR-003](docs/adr/ADR-003-consistencia-inventario.md)); un `MovimientoInventario` por cada cambio; `PoliticaDevolucion` (devolución vs merma) en el dominio | `dominio/inventario/`, `aplicacion/casodeuso/ServicioInventario.java`, `infraestructura/persistencia/InventarioRepositorioJdbc.java`, pestaña *Inventario* | Unitarias: `IngredienteTest`, `RecetaYConsumoTest`, `ServicioInventarioTest`, `GestorPedidosTest` · Integración: `InventarioRepositorioJdbcIT` (**40 hilos, 1 huevo c/u, stock 10**) · Sistema: `ApiInventarioIT` · Carga: `flujo_pedidos_k6.js` (teardown verifica stock ≥ 0) | La prueba de concurrencia exige exactamente 10 ventas, 30 rechazos, stock 0 y 10 movimientos. Sin stock: 409 con faltantes y el pedido no se crea. Carga: ver [pruebas.md §6](docs/pruebas.md#6-resultados) |
| **R2. Dividir la cuenta al gusto de las personas** *(funcionalidad nueva exigida por el reto)* | Modificabilidad (nuevas formas de dividir), exactitud (correctitud), usabilidad | Patrón Strategy (`EstrategiaDivision`: igualitaria, por consumo con platos compartidos, por porcentaje) + `DivisorCuenta` con propina proporcional; `Repartidor` por resto mayor y dinero en `long` para cuadrar al peso; invariante verificada en `DivisionCuenta` | `dominio/cuenta/`, `aplicacion/casodeuso/ServicioCuenta.java`, `infraestructura/rest/DivisionCuentaController.java`, `infraestructura/ui/DialogoDividirCuenta.java` | Unitarias: `RepartidorTest`, `DivisionCuentaTest`, `ServicioCuentaTest` · Propiedades (jqwik): `RepartidorPropiedadesTest` (2 × 500 casos) · Sistema: `ApiDivisionCuentaIT` · Carga: check "cuenta cuadra al peso" | $100.000 / 3 = 33.334 + 33.333 + 33.333; para todo total y 1..20 personas la suma es exacta; ejemplo por consumo: Ana $73.000 / Luis $41.000 |
| **R3. Analítica más completa y con gráficas** *(nuevos reportes y KPI exigidos por el reto)* | Usabilidad (visualización), modificabilidad (nuevos reportes), rendimiento de consultas | Las estrategias de reporte devuelven datos (`Reporte`) en vez de texto; 4 reportes nuevos + `Indicadores` + `Periodo`; el mismo caso de uso alimenta dos adaptadores: gráficas Java2D en Swing y JSON en `/api/analitica`. Tras el cuello de botella encontrado en carga: filtro por fecha en SQL y caché de 3 s con un solo cálculo a la vez | `dominio/reportes/`, `aplicacion/casodeuso/ServicioAnalitica.java`, `infraestructura/ui/PanelAnalitica.java`, `GraficoBarras.java`, `infraestructura/rest/AnaliticaController.java` | Unitarias: `ReportesTest`, `ServicioAnaliticaTest` (incl. 20 usuarios simultáneos → 1 cálculo) · Sistema: `ApiAnaliticaIT` · Carga: `analitica_k6.js` antes/después de cargar pedidos, sin y con caché (SLO p95 ≤ 500 ms) | Escenario fijo: ventas $125.000, ticket $62.500, 20 min, cancelación 1/3. Carga sin caché: **el servicio se cayó** con 30 usuarios y decenas de miles de pedidos (hallazgo, [perf/README.md §7](perf/README.md#7-hallazgo-de-la-primera-ejecución)); con caché: ver [pruebas.md §6](docs/pruebas.md#6-resultados) |
| **R0. Límites del Corte 1: sin persistencia y un solo usuario** | Disponibilidad de los datos, rendimiento con varios meseros, mantenibilidad (una regla, varios canales) | Arquitectura hexagonal: puertos de salida con adaptadores JDBC/H2 + HikariCP ([ADR-002](docs/adr/ADR-002-persistencia-jdbc-h2-pool.md)); API REST y POS Swing como dos adaptadores de entrada sobre los mismos casos de uso; candados por mesa/pedido en `GestorPedidos` | `aplicacion/puerto/salida/`, `infraestructura/persistencia/`, `infraestructura/rest/`, `infraestructura/ui/` | Arquitectura: `ReglasDeDependenciaTest` · Integración: `PedidoRepositorioJdbcIT` · Sistema: `ApiPedidosIT` · Carga: `flujo_pedidos_k6.js` baseline + carga (SLO p95 ≤ 500 ms, errores < 1 %) | Mesa ocupada → 409; flujo completo por HTTP → *Entregado*. Carga: ver [pruebas.md §6](docs/pruebas.md#6-resultados) |

**Lo que no se resolvió por completo** (detalle en [arquitectura.md §7](docs/arquitectura.md#7-límites-conocidos-y-trabajo-para-el-corte-3)):
pedido e inventario se coordinan con una compensación y no con una transacción única; los candados
por mesa solo protegen dentro de una instancia; la analítica recalcula sobre todo el historial en
cada recálculo (mitigado con caché, la solución de fondo es CQRS); H2 en memoria por defecto.

---

## Créditos y roles

| Integrante | Rol / contribución principal |
|---|---|
| Federico Valdez | Programador |
| Daniel Sanabria | Arquitecto |

Videos del Corte 1: [videos/](videos/).
