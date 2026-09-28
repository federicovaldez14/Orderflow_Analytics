# ADR-002 — Persistencia con JDBC plano sobre H2 y pool HikariCP

- **Estado:** aceptada
- **Fecha:** 2026-09-28

## Contexto

El Corte 1 no persistía nada (límite declarado). R1 necesita guardar stock, recetas y un historial
de movimientos; la analítica (R3) necesita historial de pedidos; las pruebas de integración piden
infraestructura real o embebida.

En el taller de pruebas de carga se midió que abrir una conexión JDBC nueva por operación
(`DriverManager.getConnection`) era el cuello de botella del servicio (defecto PERF-01) y que un pool
HikariCP lo resolvía.

## Opciones consideradas

1. Seguir en memoria (listas en Java).
2. **JDBC plano + H2 embebido + HikariCP** (el mismo estilo del `RegistryRepository` de los talleres).
3. JPA / Hibernate.
4. PostgreSQL desde el inicio.

## Decisión

Opción 2. Cada puerto de salida tiene un adaptador `*RepositorioJdbc` que recibe un `DataSource`
(HikariCP, 20 conexiones, configurado por `spring.datasource.*`). H2 en memoria por defecto, con URL
de archivo disponible en `application.properties`. Esquema creado por cada adaptador
(`initSchema()`), como en el taller.

## Consecuencias

**Positivas**
- Mismo patrón de los talleres (sin tecnología nueva que aprender) y SQL visible: se puede razonar
  exactamente qué hace la transacción del inventario (ADR-003).
- Pruebas de integración rápidas con una base H2 nueva por prueba (`BaseDeDatosH2`), sin Docker.
- Pool desde el principio: no se repite el defecto PERF-01.
- Migrar a PostgreSQL: cambiar URL y driver; el SQL usado es estándar (el `LIMIT` y la secuencia
  existen en ambos).

**Negativas**
- Mapeo manual fila ↔ objeto (más código que JPA).
- H2 no es PostgreSQL: diferencias de bloqueo y dialecto. Una prueba verde en H2 no garantiza
  producción (lección del taller de integración). Pendiente: prueba con Testcontainers.
- En memoria, los datos se pierden al reiniciar (aceptable para la demo; configurable).
