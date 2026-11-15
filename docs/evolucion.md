# Evolución del sistema en los tres cortes

Orderflow Analytics empezó como una aplicación de escritorio para tomar pedidos en un restaurante y
terminó como un sistema web desplegado en la nube, con autenticación, base de datos gestionada y un
pipeline que no deja llegar a producción un cambio que rompa las pruebas, la arquitectura, el SLO o
la seguridad. Este documento cuenta ese recorrido: qué se decidió en cada corte, qué se mantuvo, qué
cambió y por qué, y qué límites tiene el sistema final.

## 1. Resumen

| | Corte 1 — Diseño | Corte 2 — Arquitectura y pruebas | Corte 3 — Entrega continua y segura |
|---|---|---|---|
| **Pregunta** | ¿Cómo se modela el negocio? | ¿Qué estructura atiende los retos y cómo se demuestra? | ¿Cómo se garantiza que cada cambio conserve lo anterior y llegue solo a producción? |
| **Forma del sistema** | Aplicación Swing, un proceso, datos en memoria | Hexagonal: núcleo Java puro + adaptadores (Swing, REST, web, JDBC/H2) | La misma hexagonal, en un contenedor Docker, con PostgreSQL y desplegada en Render + Neon |
| **Usuarios** | Uno, en el computador del restaurante | Varios meseros en red, sin autenticación | Usuarios con roles (administrador, mesero, cocina, monitor) |
| **Persistencia** | Ninguna | H2 embebido + HikariCP (JDBC plano) | PostgreSQL 18 (Docker Compose y Neon); H2 solo para pruebas y modo escritorio |
| **Calidad demostrada con** | Diagramas UML y revisión de SOLID | 165 unitarias, integración, sistema, UI y carga con k6, ejecutadas a mano | Las mismas pruebas y más, ejecutadas en **cada** pull request y despliegue como *quality gates* |
| **Decisiones registradas** | Documento del corte ([corte1.md](corte1.md)) | ADR-001 a ADR-004 | ADR-005 (despliegue) y ADR-006 (seguridad del pipeline) |
| **Versión** | 1.x | 2.0.0 | 3.0.0 |

## 2. Corte 1 — Diseño de clases

El problema: un restaurante que toma pedidos en papel pierde comandas, no sabe cuánto inventario
gasta y no puede dividir la cuenta como piden los clientes. Se diseñó una aplicación de escritorio
con principios SOLID y patrones:

- **State** para el ciclo de vida del pedido (Recibido → En preparación → Listo → Entregado, o
  Cancelado), que impide transiciones inválidas.
- **Factory Method** para crear platos por categoría.
- **Observer** para avisar a cocina y al mesero.
- **Strategy** para los reportes de ventas.

**Límites declarados:** sin persistencia (todo se perdía al cerrar), un solo usuario, analítica en
texto, sin inventario ni división de cuenta. Los **retos** asignados para los siguientes cortes
fueron: R1 inventario con trazabilidad, R2 dividir la cuenta y R3 analítica con gráficas.

## 3. Corte 2 — Arquitectura y pruebas

- **Estilo:** arquitectura hexagonal en un solo despliegue
  ([ADR-001](adr/ADR-001-arquitectura-hexagonal.md)), elegida frente a capas, microservicios y
  eventos porque los tres retos son reglas de negocio que debían verse igual desde tres canales
  (escritorio, API y web) y probarse sin infraestructura.
- **Persistencia:** JDBC plano + H2 + HikariCP ([ADR-002](adr/ADR-002-persistencia-jdbc-h2-pool.md)).
- **Consistencia del inventario** bajo concurrencia con un `UPDATE … WHERE stock >= ?` atómico
  ([ADR-003](adr/ADR-003-consistencia-inventario.md)).
- **POS web** como tercer adaptador de entrada ([ADR-004](adr/ADR-004-interfaz-web-adaptador.md)).
- **Pruebas:**
  - Unitarias con clases de equivalencia y valores límite; propiedades con jqwik.
  - Integración con H2 y sistema por HTTP.
  - UI con Selenium.
  - Carga con k6 y un SLO (p95 ≤ 500 ms, errores < 1 %).
- **Hallazgo importante:** la carga tumbó el servicio en la analítica (`OutOfMemoryError` con
  ≈ 47.000 pedidos). Se mitigó con un filtro por fecha en SQL y una caché con un solo cálculo a la vez.

## 4. Pendientes del Corte 2 y cómo se resolvieron

Los pendientes vienen de [arquitectura.md §7](arquitectura.md#7-límites-conocidos-y-trabajo-para-el-corte-3)
y del propio README del Corte 2.

| # | Pendiente del Corte 2 | Estado | Cómo se resolvió (o por qué sigue abierto) | Evidencia |
|---|---|---|---|---|
| P1 | **Sin autenticación ni roles**: cualquiera con acceso a la API podía ajustar inventario | **Resuelto** | Spring Security con login por formulario (sesión) y HTTP Basic, usuarios en base de datos con BCrypt, 4 roles con autorización por ruta y negación por defecto, CSRF, auditoría, cabeceras de seguridad. La web muestra a cada rol solo sus pantallas | `SeguridadIT` (7 pruebas), `PosWebUiIT` flujo 5, [seguridad.md](seguridad.md) |
| P2 | **H2 en memoria** pierde los datos al reiniciar y comparte el heap con la aplicación | **Resuelto** | PostgreSQL 18 en Docker Compose y Neon en producción, con los mismos adaptadores JDBC (solo cambia la sintaxis de la secuencia, detectada al arrancar). H2 queda para pruebas rápidas | `PostgresAdaptadoresIT` (Testcontainers), [ADR-005](adr/ADR-005-despliegue-render-neon-ghcr.md) |
| P3 | **Pruebas solo en H2**: "una prueba verde en H2 no garantiza producción" (ADR-002) | **Resuelto** | Pruebas de integración contra **PostgreSQL 18 real** con Testcontainers, incluida la concurrencia del inventario (40 hilos, 10 huevos → 10 ventas, stock 0) | `PostgresAdaptadoresIT` en la etapa 3 del pipeline |
| P4 | **Las pruebas se corrían a mano**; faltaba la carga como compuerta, el análisis estático y el de dependencias | **Resuelto** | Pipeline de 8 etapas en GitHub Actions; el SLO como `thresholds` de k6 contra el entorno desplegado después de cada despliegue; CodeQL, Trivy y Gitleaks como *quality gates* | [devops.md](devops.md), historial de Actions |
| P5 | **Regla de arquitectura única** (`ReglasDeDependenciaTest`, 3 aserciones) | **Resuelto** | 8 reglas ArchUnit derivadas de los ADR (dominio aislado, núcleo sin frameworks, adaptadores de entrada sin acceso a la base, repositorios detrás de puertos, raíz de composición, sin ciclos, piezas de Spring vetadas) | `ArquitecturaTest`, etapa 4 |
| P6 | **Spring Boot 2.7 sin soporte** con 20 CVE altas y críticas (no estaba declarado: lo encontró el SCA del Corte 3) | **Resuelto** | Migración a Spring Boot 3.5 (Jakarta EE), Tomcat 10.1.62, Jackson 2.21.8 y pgjdbc 42.7.14 | [seguridad.md §4](seguridad.md#4-registro-de-hallazgos) H-01 a H-04 |
| P7 | **La versión del jar estaba fija en los comandos** (`orderflow-analytics-2.0.0.jar`) y no había versionado | **Resuelto** | Versionado semántico con tags, `CHANGELOG.md`, GitHub Releases con el `.jar` y la imagen etiquetada con la versión; `/actuator/info` muestra la versión desplegada | `release.yml`, [CHANGELOG.md](../CHANGELOG.md) |
| P8 | **Analítica no escala** a más de 200.000 pedidos (un recálculo tarda ≈ 4,8 s) | **Abierto** (mitigado) | La caché y el filtro por fecha siguen cumpliendo el SLO con el volumen de un restaurante (≈ 47.000 pedidos ≈ 1 año). La solución de fondo es un modelo de lectura (CQRS) con agregados por hora, plato y categoría; no se hizo porque el corte no pide funcionalidades nuevas y el volumen real no lo exige todavía | `analitica_k6.js` en `carga-larga.yml` |
| P9 | **Pedido e inventario no comparten una transacción** (se compensa si falla el guardado) | **Abierto** | Con PostgreSQL el riesgo baja (el guardado del pedido rara vez falla), pero sigue existiendo si el proceso muere entre las dos operaciones. Haría falta un puerto de "unidad de trabajo" | — |
| P10 | **Candados por mesa en memoria**: solo protegen dentro de una instancia | **Abierto** (no aplica hoy) | Producción corre **una** instancia (ADR-005), así que los candados son correctos. Para escalar horizontalmente haría falta bloqueo optimista (`version` en `pedido`) | — |
| P11 | **POS web por *polling*** cada ≈ 2,5 s | **Abierto** | Aceptable con pocas pantallas; la alternativa (SSE/WebSocket) además entraría en conflicto con el riesgo aceptado H-07 (SSE) | Regla R8 |
| P12 | **Las pruebas de UI no cubren la ventana Swing** | **Abierto** | La ventana de escritorio no se despliega en el servidor (el contenedor corre sin interfaz); el cliente de escritorio se publica como `.jar` en Releases | — |
| P13 | **La división de cuenta no registra pagos** | Fuera de alcance | Es un módulo de negocio nuevo y el corte no pide funcionalidades nuevas | — |

**Retroalimentación del Corte 2:** *(se completa con las observaciones del profesor)*

## 5. Qué decisiones se mantuvieron y cuáles cambiaron

| Decisión | Corte en que se tomó | Estado en el Corte 3 | Por qué |
|---|---|---|---|
| Hexagonal en un solo despliegue (ADR-001) | 2 | **Se mantiene** | Fue lo que permitió cambiar H2 por PostgreSQL y agregar seguridad sin tocar el dominio: ningún archivo de `dominio/` ni de los casos de uso cambió por PostgreSQL, Spring Security o Docker (el único cambio en el núcleo durante el Corte 3 fue corregir el hallazgo H-06 de CodeQL en `Repartidor`, 2 líneas). Las reglas ArchUnit lo garantizan desde ahora |
| Patrones del Corte 1 (State, Strategy, Factory Method, Observer) | 1 | **Se mantienen** | Siguen siendo el corazón del dominio; el Strategy de división (R2) y el de reportes (R3) se extendieron sin modificar lo existente |
| JDBC plano en vez de JPA (ADR-002) | 2 | **Se mantiene** | El SQL estándar funcionó en PostgreSQL con un solo cambio (la secuencia); la consulta atómica del inventario (ADR-003) sigue siendo visible y verificable |
| H2 como base del sistema (ADR-002) | 2 | **Cambia** → PostgreSQL ([ADR-005](adr/ADR-005-despliegue-render-neon-ghcr.md)) | Datos persistentes, sin competir por el heap, misma base en desarrollo (Compose) y producción |
| API sin autenticación | 2 | **Cambia** → Spring Security con roles | Al publicar el sistema en Internet, dejarlo abierto era la amenaza principal (STRIDE A1, A2) |
| Pruebas ejecutadas a mano | 2 | **Cambia** → pipeline con *quality gates* | Un cambio que rompe algo ya no puede llegar a `main` sin que se note |
| Spring Boot 2.7 | 1–2 | **Cambia** → 3.5 | Sin soporte y con vulnerabilidades críticas |
| SLO p95 ≤ 500 ms / errores < 1 % | 2 | **Se mantiene** y ahora se verifica en cada despliegue | Es la misma meta; el entorno gratuito es más lento que un PC y aun así la cumple (prueba corta de 5 usuarios) |

## 6. Límites conocidos del sistema final

- **Entorno gratuito:** el servicio se duerme tras 15 min sin uso y tarda ≈ 1 min en despertar;
  512 MB y 0,1 CPU. Para operar un restaurante real se recomienda el plan pagado más pequeño
  ([implementacion.md §3](implementacion.md#3-costos-estimados)).
- **Una instancia:** sin réplica; si Render cae, el sistema no responde (los datos están a salvo en
  Neon). Escalar requiere bloqueo optimista (P10).
- **Analítica sobre el historial completo** (P8) y **transacción compensada** entre pedido e
  inventario (P9).
- **Riesgo aceptado H-07** (Spring 6.2 sin parche) hasta migrar a Spring Boot 4.
- **Sin límite de intentos de login** y auditoría solo en los logs del contenedor
  ([seguridad.md §7](seguridad.md#7-riesgos-abiertos)).

## 7. Qué haríamos distinto

- **Contenedores y pipeline desde el Corte 2:** varios problemas (H2 en memoria, Spring Boot sin
  soporte, la falta de autenticación) se habrían visto antes si el SCA y el despliegue hubieran
  existido desde el principio.
- **PostgreSQL con Testcontainers desde el inicio** en vez de H2: la diferencia de dialecto fue
  pequeña, pero la duda de "¿funcionará en la base real?" acompañó todo el Corte 2.
- **Modelo de lectura para la analítica** desde el diseño del Reto 3, en lugar de calcular sobre el
  historial y mitigar con caché cuando la carga lo tumbó.
