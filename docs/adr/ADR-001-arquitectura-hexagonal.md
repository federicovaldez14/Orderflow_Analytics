# ADR-001 — Arquitectura hexagonal (puertos y adaptadores) en un solo despliegue

- **Estado:** aceptada
- **Fecha:** 2026-09-28
- **Decisores:** Federico Valdez, Daniel Sanabria

## Contexto

El sistema del Corte 1 era un módulo Swing con el dominio bien diseñado (State, Factory, Observer,
Strategy) pero sin frontera clara: la UI modificaba objetos `Pedido` y repetía reglas, los datos
vivían en memoria y solo había un cliente posible (la ventana).

Los retos del Corte 1 exigen:

- **R1 Inventario con trazabilidad:** persistir stock y movimientos, y mantenerlos consistentes con
  varios meseros tomando pedidos a la vez.
- **R2 Dividir la cuenta al gusto:** varias formas de dividir, extensibles, con resultado exacto.
- **R3 Analítica más completa y con gráficas:** más reportes, visualización y acceso por otros
  clientes.
- Y las pruebas del Corte 2 requieren probar el núcleo aislado, probar las fronteras con
  infraestructura real y medir el sistema por HTTP (k6, caja negra).

Restricciones: equipo de 2 personas, un restaurante, un despliegue; tecnologías vistas en el curso
(Java, Spring Boot, JDBC, H2, JUnit, Mockito, k6).

## Opciones consideradas

1. **Arquitectura en capas (presentación / negocio / datos).**
2. **Hexagonal / limpia (puertos y adaptadores) en un solo despliegue.**
3. **Microservicios** (pedidos, inventario, analítica por separado).
4. **Orientada a eventos** (pedidos publica eventos; inventario y analítica consumen).

La comparación con criterios derivados de los retos está en
[arquitectura.md §3](../arquitectura.md#3-comparación-de-estilos-y-decisión) (hexagonal 28, capas
21, microservicios 18, eventos 16).

## Decisión

Adoptar **arquitectura hexagonal en un solo despliegue**:

- `dominio/` — entidades y reglas (Pedido, estados, inventario, cuenta, reportes). Java puro.
- `aplicacion/` — casos de uso (`GestorPedidos`, `ServicioInventario`, `ServicioCuenta`,
  `ServicioAnalitica`) y **puertos de salida** (`PedidoRepositorio`, `MenuRepositorio`,
  `InventarioRepositorio`, `AlertaInventario`; `Notificador` es un puerto definido por el dominio).
- `infraestructura/` — **adaptadores**: de entrada (Swing, REST) y de salida (JDBC/H2,
  notificadores), más el *composition root* (`ConfiguracionOrderflow`), único lugar donde se decide
  qué adaptador implementa cada puerto.

Las dependencias solo apuntan hacia el núcleo; se verifica con `ReglasDeDependenciaTest`.

## Consecuencias

**Positivas**
- La UI Swing y la API REST llaman a los mismos casos de uso: las reglas (una comanda activa por
  mesa, no vender sin stock, la cuenta cuadra) existen una sola vez.
- El núcleo se prueba con dobles de prueba de los puertos, sin base de datos ni Spring (unitarias
  rápidas y deterministas).
- Los retos se resolvieron agregando código: nuevas estrategias (`DivisionPorConsumo`, cuatro
  reportes), un nuevo componente (`ServicioInventario`) y nuevos adaptadores, sin reescribir los
  existentes.
- Cambiar H2 por PostgreSQL, o agregar un canal de notificación (correo al proveedor cuando falta un
  ingrediente) es escribir un adaptador.

**Negativas (lo que se sacrifica)**
- Más clases e indirección: puertos, DTOs de REST, composition root. Un integrante nuevo necesita
  entender el estilo antes de ubicar el código.
- Un solo despliegue: escala verticalmente; no se puede escalar la analítica por separado.
- Pedido e inventario son dos puertos distintos: no hay una transacción que los abarque a ambos; se
  usa una compensación en `GestorPedidos` (ver límites en arquitectura.md §7).
- Mapear dominio ↔ tablas a mano (`Pedido.reconstituir`, DTOs) es código extra frente a un ORM.

## Cuándo reconsiderar

Si la analítica tuviera que atender muchos locales o su carga afectara la toma de pedidos, separar
la lectura con CQRS (y eventualmente un servicio aparte) sería el siguiente paso; la hexagonal lo
facilita porque la analítica ya depende solo de un puerto.
