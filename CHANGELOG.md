# Changelog

Todos los cambios relevantes de Orderflow Analytics. Formato basado en
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
[versionado semántico](https://semver.org/lang/es/): `MAYOR` cuando algo deja de ser compatible,
`MENOR` para funcionalidad nueva compatible, `PARCHE` para correcciones.

Desde la 3.0.0 cada versión tiene un tag `vX.Y.Z`, un GitHub Release con el `.jar` y la imagen
`ghcr.io/federicovaldez14/orderflow-analytics:vX.Y.Z` (ver [docs/devops.md](docs/devops.md)).

## [3.0.0] - 2026-11-15

Corte 3: entrega continua y segura. Sin funcionalidades de negocio nuevas.

### Agregado
- Inicio de sesión con usuarios y roles (`ADMIN`, `MESERO`, `COCINA`, `MONITOR`): pantalla de
  login en el POS web, cada rol ve solo sus secciones, gestión de usuarios en `/api/usuarios`,
  protección CSRF, auditoría de cambios y cabeceras de seguridad.
- Soporte de PostgreSQL 18 (Docker Compose y Neon) con los mismos adaptadores JDBC; pruebas contra
  PostgreSQL real con Testcontainers.
- Pipeline de GitHub Actions con 8 etapas: construcción, unitarias con cobertura, integración, reglas
  de arquitectura, UI, seguridad (CodeQL, Trivy, Gitleaks), imagen escaneada y despliegue en Render
  con prueba de humo y prueba de carga contra el SLO.
- Workflows de versión (tag → imagen versionada + Release), reversión y carga larga; Dependabot.
- 8 reglas de arquitectura con ArchUnit (antes 1 prueba con 3 aserciones).
- `/actuator/info` con la versión desplegada; prueba de humo `perf/scripts/humo_k6.js`.
- Observabilidad: Prometheus y Grafana con un tablero del SLO (`docker compose --profile observabilidad up`).
- Documentación: `docs/devops.md`, `docs/seguridad.md`, `docs/evolucion.md`,
  `docs/implementacion.md`, ADR-005 y ADR-006, vista de despliegue en `docs/arquitectura.md`.

### Cambiado
- Spring Boot 2.7.18 → 3.5.16 (Jakarta EE); Tomcat 10.1.62, Jackson 2.21.8 y pgjdbc 42.7.14 para
  corregir vulnerabilidades altas y críticas.
- Docker Compose levanta la aplicación con PostgreSQL (antes H2 en archivo) y con el contenedor
  endurecido (solo lectura, sin capacidades, límite de memoria).
- La base de datos de producción es PostgreSQL; H2 queda para pruebas y para el POS de escritorio sin servidor.

### Corregido
- Posible bucle infinito en el reparto de la cuenta si el sobrante superara el rango de `int`
  (hallazgo de CodeQL).
- Inyección en el log de auditoría con saltos de línea (CWE-117, hallazgo de CodeQL).

### Seguridad
- Riesgo aceptado hasta 2027-02-28: CVE-2026-47884 y CVE-2026-47890 de Spring Framework 6.2 (sin
  parche en esa rama; el sistema no usa las piezas afectadas y una regla de arquitectura lo impide).
  Ver `docs/seguridad.md`.

## [2.0.0] - 2026-10-04

Corte 2: arquitectura hexagonal y pruebas. Versión sin tag.

### Agregado
- Reto 1: inventario de ingredientes con recetas, descuento atómico por pedido, devoluciones,
  mermas, ajustes, alertas de stock bajo y trazabilidad de movimientos.
- Reto 2: división de la cuenta igualitaria, por consumo (con platos compartidos) y por porcentaje,
  con propina proporcional y reparto exacto al peso.
- Reto 3: analítica con 6 reportes, indicadores, filtro por periodo y gráficas (Swing y web).
- API REST, POS web (tercer adaptador de entrada) y persistencia JDBC con H2 y HikariCP.
- Pruebas unitarias, de propiedades, de integración, de sistema, de UI (Selenium) y de carga (k6).
- Contenerización: Dockerfile multi-stage, `.dockerignore` y Docker Compose.

### Cambiado
- El sistema del Corte 1 se reorganizó en arquitectura hexagonal (ADR-001).

## [1.0.0] - 2026-08-23

Corte 1: diseño de clases. Versión sin tag.

### Agregado
- Aplicación de escritorio (Swing) para tomar pedidos por mesa, con ciclo de vida del pedido
  (State), creación de platos (Factory Method), avisos a cocina y mesero (Observer) y reportes de
  ventas (Strategy).
