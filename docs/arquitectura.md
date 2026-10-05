# Documento de arquitectura — Orderflow Analytics (Corte 2)

> Diseño y Arquitectura de Software (DYAS) · Universidad de La Sabana
> Integrantes: Federico Valdez, Daniel Sanabria

Contenido

1. [Descripción del sistema y resumen del Corte 1](#1-descripción-del-sistema-y-resumen-del-corte-1)
2. [Retos asignados y tabla de trazabilidad](#2-retos-asignados-y-tabla-de-trazabilidad)
3. [Comparación de estilos y decisión (ADR)](#3-comparación-de-estilos-y-decisión)
4. [Arquitectura inicial (Corte 1) y evolucionada (Corte 2)](#4-arquitectura-inicial-corte-1-y-evolucionada-corte-2)
5. [Estrategia de pruebas](#5-estrategia-de-pruebas)
6. [Resultados de las pruebas](#6-resultados-de-las-pruebas)
7. [Límites conocidos y trabajo para el Corte 3](#7-límites-conocidos-y-trabajo-para-el-corte-3)

---

## 1. Descripción del sistema y resumen del Corte 1

**Problema.** En un restaurante que trabaja con papel y voz, las comandas se pierden o se duplican,
nadie sabe cuánto tarda un pedido y el dueño decide el menú "a ojo".

**Corte 1 (entregado).** Un módulo de gestión de pedidos en Java con principios SOLID y cuatro
patrones:

| Patrón | Dónde (Corte 1) | Para qué |
|---|---|---|
| Factory Method | `PlatoFactory` | Crear platos por categoría con su tiempo de preparación |
| State | `EstadoPedido` + 4 estados (+ `EstadoCancelado`) | Flujo Creado → En preparación → Listo → Entregado |
| Observer | `Notificador`, `NotificadorCocina`, `NotificadorMesero` | Avisar a cocina y mesero en cada cambio |
| Strategy | `EstrategiaReporte` + 3 reportes | Platos más/menos pedidos, tiempo promedio |

Tenía una UI Swing (mapa de mesas, lista, notificaciones, analítica en texto), **todo en memoria**
y un único proceso. La propia documentación del Corte 1 declaraba dos límites: sin persistencia y
sin concurrencia real.

**Corte 2 (este entregable).** El mismo dominio, reorganizado en **arquitectura hexagonal**, con
persistencia en H2, tres adaptadores de entrada sobre los mismos casos de uso (POS de escritorio
Swing, API REST y POS web), los tres retos resueltos (inventario, división de cuenta, analítica con
gráficas) y pruebas unitarias, de arquitectura, de integración, de sistema, de carga y de UI, más una
evaluación heurística de UX.

---

## 2. Retos asignados y tabla de trazabilidad

Retos recibidos en el Corte 1:

| # | Reto (enunciado del profesor) | Qué exige en términos del sistema |
|---|---|---|
| R1 | "Manejar un apartado de inventario para tener trazabilidad de los ingredientes" | Cada plato vendido debe descontar sus ingredientes; no se puede vender lo que no hay; todo cambio de stock debe quedar registrado con su causa (venta, devolución, merma, compra, ajuste) y el pedido que lo originó. Con varios meseros a la vez, el stock no puede quedar inconsistente. |
| R2 | "Si son varias personas, que la cuenta pueda dividirse al gusto de las personas" | Dividir un mismo pedido de varias formas (iguales, por lo que consumió cada uno con platos compartidos, por porcentaje), con propina opcional, y que las partes sumen **exactamente** el total. Debe poder agregarse otra forma de dividir sin reescribir las existentes. |
| R3 | "Que la analítica se vea mejor, que muestre gráficas, más completa" | Pasar de reportes en texto a indicadores y gráficas; más reportes (horas pico, categorías, tiempos por etapa); filtrables por periodo; y disponibles también para otros clientes (API). |
| R0 | Límites declarados en Corte 1: sin persistencia, un solo usuario | Los datos deben sobrevivir y el sistema debe atender a varios meseros (y clientes HTTP) al mismo tiempo. Es la base técnica que R1 y la prueba de carga necesitan. |

La tabla de trazabilidad completa (reto → atributo → decisión → dónde → prueba → resultado) está en
el [README](../README.md#tabla-de-trazabilidad) para que sea lo primero que se vea.

**Funcionalidad nueva agregada.** El enunciado solo permite agregar funcionalidad cuando un reto lo
exige: inventario (R1), división de cuenta (R2) y los nuevos reportes/KPI (R3) están en esa
situación y están marcados en la tabla de trazabilidad. La API REST y el POS web no son
funcionalidad de negocio nueva: son adaptadores de entrada sobre los mismos casos de uso (la API la
necesitan las pruebas de caja negra y de carga; la web responde a la usabilidad de R3 y permite
pruebas de UI; ver [ADR-004](adr/ADR-004-interfaz-web-adaptador.md)).

---

## 3. Comparación de estilos y decisión

### 3.1 Criterios (derivados de los retos)

| Criterio | Viene de | Atributo de calidad |
|---|---|---|
| K1. Agregar formas de dividir, reportes o canales sin tocar lo existente | R2, R3 | Modificabilidad / extensibilidad |
| K2. Varios adaptadores de entrada (POS Swing, API REST, POS web, k6) con **las mismas reglas** | R0, R3 | Mantenibilidad (no duplicar reglas) |
| K3. Probar reglas de negocio aisladas (reparto al peso, política de devolución, stock) | R1, R2 | Testabilidad |
| K4. Consistencia del inventario con pedidos concurrentes | R1, R0 | Integridad de datos / confiabilidad |
| K5. Cambiar la tecnología de persistencia (H2 → PostgreSQL) sin tocar el negocio | R0 | Portabilidad / modificabilidad |
| K6. Proporcional al tamaño real: 2 personas, un restaurante, un despliegue | Enunciado | Simplicidad / costo |

### 3.2 Matriz de estilos candidatos (1 = malo, 5 = muy bueno)

| Criterio | Capas (3 niveles) | **Hexagonal / limpia** (monolito) | Microservicios (pedidos, inventario, analítica) | Orientada a eventos |
|---|---|---|---|---|
| K1 Extensibilidad | 3 — posible, pero la lógica tiende a filtrarse al controlador | **5** — Strategy en el dominio + adaptadores nuevos | 4 | 4 — nuevos consumidores de eventos |
| K2 Varios adaptadores, mismas reglas | 3 — la capa de presentación suele ser una sola | **5** — es exactamente el caso de uso de puertos de entrada | 4 | 3 |
| K3 Testabilidad aislada | 3 — el servicio depende del DAO concreto | **5** — el núcleo no depende de nada; mocks de puertos | 4 por servicio, 2 extremo a extremo | 2 — flujos asíncronos difíciles de probar |
| K4 Consistencia del inventario | 4 — una BD, transacción local | **4** — una BD, transacción local en el adaptador | **1** — pedido e inventario en BD distintas: requiere sagas / compensaciones distribuidas | 2 — consistencia eventual; vender el último huevo dos veces es posible |
| K5 Cambiar persistencia | 3 — la capa de datos se cambia, pero el servicio suele conocer el DAO | **5** — nuevo adaptador del mismo puerto | 4 | 3 |
| K6 Proporcionalidad | **5** | 4 — algunas clases más (puertos) | **1** — 3 despliegues, red, descubrimiento, trazas distribuidas para 2 desarrolladores | 2 — requiere broker |
| **Total** | 21 | **28** | 18 | 16 |

### 3.3 Decisión

Se eligió **arquitectura hexagonal (puertos y adaptadores) en un solo despliegue** — un "monolito
bien modularizado". Queda registrada en
[ADR-001](adr/ADR-001-arquitectura-hexagonal.md). Decisiones que se derivan de ella:

- [ADR-002](adr/ADR-002-persistencia-jdbc-h2-pool.md) — persistencia con JDBC plano sobre H2 y pool HikariCP.
- [ADR-003](adr/ADR-003-consistencia-inventario.md) — cómo se garantiza que el inventario no se sobrevenda con pedidos concurrentes.
- [ADR-004](adr/ADR-004-interfaz-web-adaptador.md) — interfaz web como tercer adaptador de entrada (y por qué no una SPA aparte).

**Qué se gana:** reglas en un solo lugar para Swing, REST, web y pruebas; núcleo probado sin base de
datos; persistencia reemplazable; los retos se resuelven agregando clases (estrategias, adaptadores),
no editando las existentes.

**Qué se sacrifica:** más clases e indirección (puertos, DTOs, composition root); un solo despliegue
escala solo verticalmente; la consistencia entre pedido e inventario se resuelve con una
compensación en el caso de uso (no hay una única transacción que abarque ambos, ver §7).

**Por qué no microservicios:** ningún reto exige escalar o desplegar partes por separado, y R1 (no
sobrevender) es mucho más difícil con pedidos e inventario en bases distintas. Sería una mala
decisión aunque estuviera bien implementada (lo advierte el enunciado).

**Qué se tomó de otros estilos:** de capas, la idea de que el adaptador REST solo traduce; de
eventos, el patrón Observer interno (notificadores y alertas de stock) — dentro del proceso, sin
broker.

---

## 4. Arquitectura inicial (Corte 1) y evolucionada (Corte 2)

### 4.1 Corte 1 — antes

```mermaid
flowchart LR
    UI["UI Swing<br/>(PosApp y paneles)"] -->|"modifica Pedido directamente<br/>y valida reglas en la UI"| M["modelo / estado / fabrica"]
    UI --> G["GestorPedidos<br/>(lista en memoria)"]
    G --> M
    UI --> R["reportes (String)"]
    M --> O["observador<br/>(System.out)"]
```

Problemas que motivaron el cambio: la UI cambiaba objetos del dominio y repetía reglas (no quitar
el último plato, no cancelar un pedido terminado); no había persistencia ni forma de exponer el
sistema a otro cliente; los reportes devolvían texto, imposible de graficar.

### 4.2 Corte 2 — Diagrama de contexto (C4 nivel 1)

```mermaid
flowchart TB
    mesero(["👤 Mesero<br/>toma y edita pedidos, divide la cuenta"])
    cocina(["👤 Cocina<br/>ve y avanza comandas"])
    admin(["👤 Administrador / dueño<br/>inventario y analítica"])
    cliente(["🖥️ Cliente HTTP<br/>(k6, pruebas de sistema, futuros tableros)"])
    sistema["<b>Orderflow Analytics</b><br/>Sistema de pedidos, inventario, división de cuenta y analítica del restaurante"]
    fuentes["Google Fonts<br/><i>sistema externo, opcional</i>"]
    mesero -->|"POS de escritorio o navegador<br/>(PC, tablet, celular)"| sistema
    cocina -->|"navegador o escritorio"| sistema
    admin -->|"navegador o escritorio"| sistema
    cliente -->|"JSON / HTTP"| sistema
    sistema -.->|"tipografía de la web"| fuentes
```

### 4.3 Diagrama de contenedores (C4 nivel 2)

```mermaid
flowchart TB
    subgraph navegador["Navegador (PC, tablet o celular)"]
        web["POS web<br/><i>HTML + CSS + JavaScript (módulos ES)</i><br/>salón, cocina, inventario, analítica, actividad"]
    end
    subgraph jvm["Proceso Java 17 — java -jar orderflow-analytics.jar"]
        pos["POS de escritorio<br/><i>Swing + FlatLaf</i><br/>mapa de mesas, pedidos, inventario, analítica"]
        est["Archivos estáticos<br/><i>Spring Boot, /</i><br/>index.html, css, js"]
        api["API REST<br/><i>Spring Boot / Tomcat, puerto 8080</i><br/>/api/pedidos, /api/inventario, /api/analitica, /api/notificaciones"]
        nucleo["Núcleo de la aplicación<br/><i>Java puro</i><br/>casos de uso + dominio"]
        act["Actuator + Prometheus<br/><i>/actuator</i>"]
        pos -->|"llamadas Java"| nucleo
        api -->|"llamadas Java"| nucleo
    end
    db[("Base de datos H2<br/><i>embebida, JDBC + HikariCP</i><br/>pedidos, carta, inventario, movimientos")]
    nucleo -->|"puertos de salida → adaptadores JDBC"| db
    usuarios(["Mesero · Cocina · Administrador"]) --> pos
    usuarios --> web
    est -->|"descarga la web (HTTP)"| web
    web -->|"HTTP/JSON cada ~2,5 s"| api
    http(["k6 · pruebas de sistema · Selenium"]) -->|HTTP/JSON| api
    http -->|métricas| act
```

El POS web no es un despliegue aparte: Spring Boot sirve sus archivos desde
`src/main/resources/static/` en el mismo puerto, y el navegador solo habla con la API REST
([ADR-004](adr/ADR-004-interfaz-web-adaptador.md)). H2 corre dentro del mismo proceso (modo embebido). Por eso es un contenedor lógico distinto
(esquema, transacciones) pero no un despliegue separado; cambiarlo por PostgreSQL es cambiar la URL
y el driver (ADR-002).

### 4.4 Diagrama de componentes (C4 nivel 3) — la hexagonal

```mermaid
flowchart LR
    subgraph entrada["Adaptadores de ENTRADA<br/>infraestructura/ui · infraestructura/rest · static/"]
        ui["Swing: PosApp, PanelMapaMesas,<br/>PanelInventario, PanelAnalitica,<br/>DialogoDividirCuenta"]
        rest["REST: PedidoController, InventarioController,<br/>DivisionCuentaController, AnaliticaController,<br/>NotificacionesController, ManejadorErrores"]
        web["Web (navegador): api.js + vistas/<br/>salon, cocina, inventario,<br/>analitica, actividad"]
    end
    subgraph app["APLICACIÓN — aplicacion/"]
        gp["GestorPedidos"]
        si["ServicioInventario<br/>(implementa ControlInventario)"]
        sc["ServicioCuenta"]
        sa["ServicioAnalitica"]
        subgraph puertos["puerto/salida"]
            ppr["PedidoRepositorio"]
            pmr["MenuRepositorio"]
            pir["InventarioRepositorio"]
            pal["AlertaInventario"]
        end
    end
    subgraph dom["DOMINIO — dominio/"]
        modelo["modelo: Pedido, ItemPedido, Plato"]
        estado["estado: State"]
        fab["fabrica: PlatoFactory"]
        inv["inventario: Ingrediente, Receta,<br/>CalculadoraConsumo, PoliticaDevolucion"]
        cta["cuenta: EstrategiaDivision (Strategy),<br/>DivisorCuenta, Repartidor"]
        rep["reportes: EstrategiaReporte (Strategy),<br/>Indicadores, Periodo"]
        obs["observador: Notificador (puerto)"]
    end
    subgraph salida["Adaptadores de SALIDA — infraestructura/"]
        jdbc["persistencia: PedidoRepositorioJdbc,<br/>MenuRepositorioJdbc, InventarioRepositorioJdbc"]
        noti["notificacion: NotificadorCocina,<br/>NotificadorMesero, AlertasInventarioEnMemoria"]
    end
    cfg["config: ConfiguracionOrderflow<br/>(composition root), DatosIniciales"]

    ui --> gp & si & sc & sa
    rest --> gp & si & sc & sa
    web -->|"HTTP/JSON"| rest
    rest -->|"lee avisos"| noti
    gp --> si
    gp & si & sc & sa --> dom
    gp --> ppr & pmr
    si --> pir & pal
    sc --> ppr
    sa --> ppr
    jdbc -. implementa .-> ppr & pmr & pir
    noti -. implementa .-> pal & obs
    cfg -. crea y conecta .-> app
```

**Regla del estilo:** las flechas de dependencia apuntan hacia adentro. `dominio` no importa nada de
`aplicacion` ni de `infraestructura` (ni Spring, JDBC o Swing); `aplicacion` no importa
`infraestructura`. Lo verifica automáticamente
[`ReglasDeDependenciaTest`](../src/test/java/com/restaurant/arquitectura/ReglasDeDependenciaTest.java):
si alguien rompe la regla, el build falla. Spring solo aparece en `infraestructura` (y en
`OrderflowApplication`); el núcleo se construye con `new` en `ConfiguracionOrderflow`.

### 4.5 Correspondencia con el código

| Componente | Paquete | Estilo / patrón |
|---|---|---|
| Entidades y reglas | `com.restaurant.dominio.*` | Núcleo hexagonal; State, Factory Method, Strategy (x2), Observer |
| Casos de uso | `com.restaurant.aplicacion.casodeuso` | Servicios de aplicación |
| Puertos de salida | `com.restaurant.aplicacion.puerto.salida` + `dominio.observador.Notificador` | Puertos |
| Adaptadores de entrada | `com.restaurant.infraestructura.ui` (Swing), `...rest` (HTTP), `src/main/resources/static/` (web, cliente de la API) | Adaptadores primarios |
| Adaptadores de salida | `com.restaurant.infraestructura.persistencia`, `...notificacion` | Adaptadores secundarios |
| Ensamblado | `com.restaurant.infraestructura.config`, `OrderflowApplication` | Composition root |

Diagrama de clases del dominio: [`diagramas/clases-dominio.md`](diagramas/clases-dominio.md).
Secuencia "tomar un pedido con inventario": [`diagramas/secuencia-crear-pedido.md`](diagramas/secuencia-crear-pedido.md).

---

## 5. Estrategia de pruebas

Resumen (el detalle, con cada clase de prueba y su propósito, está en [`pruebas.md`](pruebas.md)):

| Nivel | Qué se prueba | Por qué ahí | Herramientas | Comando |
|---|---|---|---|---|
| Unitarias | Reglas del dominio y casos de uso, con puertos simulados | Es donde viven las reglas de los retos; rápidas y sin infraestructura | JUnit 5, Mockito, jqwik, JaCoCo | `mvn test` |
| Arquitectura | Que el núcleo no dependa de infraestructura | Evidencia de coherencia estilo ↔ código | JUnit 5 (lectura de imports) | `mvn test` |
| Integración | Adaptador JDBC ↔ H2 real; concurrencia del inventario | La atomicidad y el SQL solo se prueban con la base de datos de verdad | JUnit 5, H2 | `mvn verify` |
| Sistema (caja negra) | Flujos completos por HTTP: pedidos, inventario, división, analítica | Verifica todas las fronteras juntas, como un cliente | Spring Boot Test, TestRestTemplate | `mvn verify` |
| Carga | Flujo del mesero y panel de analítica con muchos usuarios | SLO de los retos bajo concurrencia | k6 | `perf/run-perf.ps1` |
| UI (bonificación) | Flujos principales en el POS web: comanda → cocina → entrega, dividir cuenta, alerta de stock | La interfaz es otro adaptador: se prueba como la usa una persona | Selenium + Page Object Model | `mvn verify -Pui-tests` |
| UX (bonificación) | Usabilidad del POS web | Hallazgos priorizados y mejoras verificadas | Evaluación heurística de Nielsen + protocolo SUS | [`ux/`](../ux/) |

---

## 6. Resultados de las pruebas

Detalle, reportes y evidencia en [`pruebas.md` §6](pruebas.md#6-resultados),
[`evidencias/`](evidencias/) y [`../perf/README.md`](../perf/README.md#6-resultados).

| Qué | Resultado |
|---|---|
| Unitarias + regla de arquitectura (`mvn test`) | **165 / 165** pasan |
| Integración y sistema (`mvn verify`) | **32 / 32** pasan |
| UI con Selenium (`mvn verify -Pui-tests`) | **4 / 4** pasan |
| Cobertura del núcleo (JaCoCo) | **96,8 % de líneas**, 87,6 % de ramas (mínimo del build: 80 %) |
| Carga — flujo del mesero, 50 usuarios | 3.700–4.600 req/s, **p95 22–29 ms**, 0 % de errores, 0 ingredientes negativos → cumple |
| Carga — analítica, ≈ 47.000 pedidos, 30 usuarios | sin caché: **caída** por memoria → con la mitigación: **p95 7,7 ms, 0 % errores** → cumple |
| Carga — analítica, > 200.000 pedidos | **no escala**: un solo cálculo tarda ≈ 4,8 s o agota la memoria (límite declarado en §7) |

**Cuello de botella y lo que ofrece la arquitectura.** El panel de analítica es O(n): materializa
todo el historial del periodo. La hexagonal permitió mitigarlo sin tocar el dominio ni los
controladores (filtro en SQL en el adaptador; caché con un solo cálculo a la vez en el caso de uso;
H2 en archivo cambiando solo la configuración). La solución de fondo —un modelo de lectura con
totales precalculados (CQRS)— entra como un puerto de lectura nuevo con su adaptador.

---

## 7. Límites conocidos y trabajo para el Corte 3

| Límite | Consecuencia | Qué haría falta |
|---|---|---|
| Pedido e inventario no comparten una transacción. El caso de uso descuenta inventario y luego guarda el pedido; si el guardado falla, **compensa** devolviendo lo descontado. | Si el proceso muere justo entre las dos operaciones, queda stock descontado sin pedido (se detecta en el historial: SALIDA_VENTA de un pedido inexistente). | Un puerto de "unidad de trabajo" que abra una transacción compartida por ambos adaptadores. |
| Los candados por pedido/mesa de `GestorPedidos` viven en memoria. | Correctos con una instancia; con varias instancias de la app detrás de un balanceador no protegen. (El inventario sí es seguro entre instancias: lo protege la base de datos.) | Bloqueo optimista con columna `version` en `pedido`. |
| La analítica calcula sobre los pedidos del periodo en cada recálculo (O(n)). Sin caché, 30 consultas simultáneas con ≈ 47.000 pedidos **tumban el servicio** (`OutOfMemoryError`). | Mitigado con filtro por fecha en SQL y caché de 3 s con un solo cálculo a la vez: con ≈ 47.000 pedidos, p95 = 7,7 ms. **No resuelto** para historiales de más de 200.000 pedidos: un recálculo tarda ≈ 4,8 s (H2 en archivo) o agota el heap (H2 en memoria). | CQRS: un modelo de lectura con agregados por hora/plato/categoría actualizado al cerrar cada pedido (puerto de lectura nuevo). |
| H2 en memoria comparte el heap con la aplicación. | Con cientos de miles de pedidos, los datos ocupan la memoria que necesitan los cálculos. | H2 en archivo (ya probado, `-BaseDatos archivo`) o PostgreSQL. |
| El POS web consulta la API cada ~2,5 s (*polling*). | Con muchas pantallas abiertas genera carga constante sobre la API. | Empujar cambios con WebSocket o Server-Sent Events ([ADR-004](adr/ADR-004-interfaz-web-adaptador.md)). |
| H2 embebido en memoria por defecto. | Al reiniciar se pierden los datos (existe la URL de archivo en `application.properties`). | Adaptador a PostgreSQL (mismo código JDBC) y prueba con Testcontainers como en el taller de integración. |
| La división de cuenta es una consulta: no registra pagos. | No hay caja ni conciliación. | Módulo de pagos (fuera del alcance de los retos). |
| Sin autenticación ni roles. | Cualquiera con acceso a la API puede ajustar inventario. | Spring Security / API key — parte de DevSecOps en el Corte 3. |
| Las pruebas de UI cubren el POS web, no la ventana Swing. | Un cambio que rompa solo Swing no lo detectan las pruebas automáticas. | AssertJ-Swing para la ventana de escritorio. |

**Corte 3:** el workflow `.github/workflows/ci.yml` ya corre `mvn verify` y las pruebas de UI en
cada push; falta agregar las pruebas de carga como etapa del pipeline (con los SLO como compuerta),
análisis estático y de dependencias, y autenticación (DevSecOps).
