# Sistema de Gestión de Pedidos para Restaurante — Corte Uno

## 1. Presentación del Problema

En un restaurante físico que opera de forma manual (papel y voz), las
comandas viajan de mesero a cocina sin ningún registro central. Esto
genera tres problemas recurrentes:

- **Errores de comunicación**: comandas olvidadas, duplicadas o mal
  anotadas entre el mesero y la cocina.
- **Falta de visibilidad del servicio**: nadie sabe cuánto tarda realmente
  un pedido en pasar de "tomado" a "entregado" hasta que el cliente se
  queja.
- **Decisiones "a ojo"**: el dueño ajusta el menú y el personal por
  intuición, sin datos de qué platos se piden más o menos.

**¿A quién afecta?** A los meseros (que cargan la responsabilidad de
recordar todo verbalmente), a la cocina (que recibe comandas ambiguas) y,
en última instancia, al cliente, que percibe demoras y errores.

**¿Por qué resolverlo con software y no con otro medio?** Un tablero de
papel o una pizarra no puede calcular automáticamente tiempos de
atención ni acumular estadísticas de consumo a lo largo de días o
semanas; un sistema de software sí, y además puede notificar en tiempo
real a cada área sin depender de que alguien grite la orden en la
cocina.

**Alcance de este módulo (Corte 1).** Este entregable cubre el módulo de
**gestión de pedidos**: creación de una comanda, su flujo de estados
(`Creado → En preparación → Listo → Entregado`), el registro automático
de tiempos, y reportes básicos de consumo (platos más/menos pedidos y
tiempo promedio de atención). **Queda explícitamente fuera de alcance**
en este corte: autenticación de usuarios, persistencia en base de datos
(los datos viven en memoria durante la ejecución de la demo), interfaz
gráfica o web, facturación/pagos, y gestión de inventario. El sistema
completo (con esos módulos) es una evolución futura; este corte
demuestra el núcleo de diseño de un módulo funcional.

## 2. Creatividad en la Presentación

🎥 `[enlace aquí — video/cómic/historia que explique el problema a una audiencia no técnica]`

## 3. Fundamentos de Ingeniería de Software

| Atributo de calidad | ¿Cómo se sostiene en el diseño? (evidencia concreta) | ¿Qué se sacrificó a cambio? |
|---|---|---|
| **Mantenibilidad** | El flujo de estados usa el patrón *State* (`EstadoPedido` y sus 4 implementaciones). Agregar un estado nuevo, por ejemplo "Cancelado", no exige tocar `Pedido` ni las clases de estado existentes, solo crear una clase nueva. | Más clases pequeñas que rastrear (4 estados en vez de 1 campo `String`); alguien nuevo en el proyecto necesita entender el patrón antes de seguir el flujo. |
| **Extensibilidad / Escalabilidad** | `PlatoFactory` centraliza la creación de platos por categoría. Agregar una categoría nueva (p. ej. `ENSALADA`) es un `case` más en la fábrica, sin tocar `Pedido`, `GestorPedidos` ni `Main`. | La fábrica crece con cada categoría nueva; si el número de tipos fuera muy grande convendría migrar a un registro configurable en vez de un `switch`. |
| **Bajo acoplamiento** | `Pedido` notifica cambios de estado a través de la interfaz `Notificador` (patrón *Observer*), sin conocer si el receptor es la pantalla de cocina, la app del mesero, o un canal futuro (p. ej. una notificación push al celular del cliente). | Indirección adicional: para saber qué pasa realmente al notificar hay que revisar cada implementación de `Notificador` por separado. |
| **Reusabilidad** | Las estrategias de reporte (`EstrategiaReporte`) son intercambiables y reutilizan lógica común (`ReportePlatosMenosPedidos` reutiliza el conteo de `ReportePlatosMasPedidos`). | Si las estrategias necesitaran compartir más lógica, ese acoplamiento entre reportes concretos podría crecer y ensuciar el "solo lo necesario" de Strategy. |

## 4. Diseño de Software

### 4.1 Principios SOLID aplicados

**Open/Closed Principle — con comparación antes/después**

```text
❌ ANTES (violación): Pedido guarda el estado como un String y el método
   avanzarEstado() tiene un if/else (o switch) que decide manualmente
   cuál es "el siguiente estado":

   public void avanzarEstado() {
       if (estado.equals("Creado")) estado = "En preparación";
       else if (estado.equals("En preparación")) estado = "Listo";
       else if (estado.equals("Listo")) estado = "Entregado";
   }

   Problema: cada vez que el restaurante necesita un estado nuevo (p. ej.
   "Cancelado" o "En espera de insumo"), hay que abrir y modificar este
   método, con el riesgo de romper transiciones que ya funcionaban.

✅ DESPUÉS (aplicando el principio): se extrae la interfaz EstadoPedido y
   cada estado concreto (EstadoCreado, EstadoEnPreparacion, EstadoListo,
   EstadoEntregado) sabe cuál es su propio siguiente estado:

   public class EstadoCreado implements EstadoPedido {
       public void avanzar(Pedido pedido) {
           pedido.cambiarEstado(new EstadoEnPreparacion());
       }
   }

   Por qué resuelve el problema: Pedido queda cerrado a modificación
   (nunca más se edita para soportar un estado nuevo) pero abierto a
   extensión (se agrega una clase EstadoCancelado sin tocar nada más).
```

**Single Responsibility Principle** — cada clase tiene un único motivo de
cambio: `Pedido` solo administra sus propios datos y delega en su estado
actual (`com.restaurant.modelo.Pedido`); `GestorPedidos` solo orquesta la
creación/avance de pedidos (`com.restaurant.servicio.GestorPedidos`);
`ReporteService` solo genera reportes (`com.restaurant.reportes.ReporteService`);
`PlatoFactory` solo sabe construir platos (`com.restaurant.fabrica.PlatoFactory`).
Se eligió esta separación y no una única clase "Restaurante" porque cada
una de estas responsabilidades cambia por razones distintas: las reglas
de reporte cambian por decisiones de negocio, las de creación de platos
por cambios de menú, y el flujo de pedidos por procesos operativos.

**Interface Segregation Principle** — `Notificador` (un solo método,
`notificar`) y `EstrategiaReporte` (un solo método, `generar`) son
interfaces pequeñas y específicas. No existe una interfaz "gorda" tipo
`IServicioRestaurante` con notificar/reportar/facturar juntos, porque eso
obligaría a `NotificadorCocina` a implementar métodos de reportes que
nunca usa.

**Dependency Inversion Principle** — `GestorPedidos` depende de la
abstracción `List<Notificador>`, no de `NotificadorCocina` o
`NotificadorMesero` en concreto (`com.restaurant.servicio.GestorPedidos`,
constructor). `ReporteService` depende de `EstrategiaReporte`, no de una
estrategia concreta (`com.restaurant.reportes.ReporteService`). Esto
permite, en pruebas o en una extensión futura, inyectar un `Notificador`
falso o una estrategia nueva sin modificar estas clases.

### 4.2 Patrones de diseño utilizados

| Patrón | Categoría | Problema que resuelve aquí | Por qué no se usó [alternativa] |
|---|---|---|---|
| **Factory Method** (`PlatoFactory`) | Creacional | Crear objetos `Plato` de distintas categorías (entrada, fuerte, bebida, postre) sin que el código cliente conozca reglas de negocio como el tiempo de preparación por defecto de cada categoría. | Se descartó Builder porque un `Plato` no se arma por pasos opcionales encadenados; es una variante de tipo seleccionada por categoría, el caso de uso típico de Factory Method. |
| **State** (`EstadoPedido` y sus 4 implementaciones) | Comportamiento | Modelar el flujo `Creado → En preparación → Listo → Entregado` sin un campo `String` y un `if/else` que crece cada vez que se agrega un estado (ver 4.1). | Se descartó una máquina de estados basada en `enum` con un `switch` centralizado porque reintroduce exactamente el problema de OCP que el patrón State evita: cada estado nuevo obligaría a tocar ese `switch`. |
| **Observer** (`Notificador`, `NotificadorCocina`, `NotificadorMesero`) | Comportamiento | Avisar a cocina y al mesero cada vez que un pedido cambia de estado, sin que `Pedido` conozca los canales concretos de notificación. | Se descartó que `Pedido` llamara directamente a `System.out.println` en cocina y mesero, porque eso acoplaría el modelo de dominio a la forma de mostrar la notificación (consola, app, impresora). |
| **Strategy** (`EstrategiaReporte` y sus 3 implementaciones) | Comportamiento | Generar distintos reportes de consumo (más pedidos, menos pedidos, tiempo promedio) de forma intercambiable, sin un método gigante con banderas `tipoReporte == 1`. | Se descartó un único método `generarReporte(int tipo)` con `switch` porque, igual que en el caso de State, cada reporte nuevo obligaría a modificar ese método en vez de solo agregar una clase. |

> El mínimo pedido por la rúbrica es 2 patrones (uno creacional y uno
> estructural o de comportamiento). Este proyecto incluye 1 creacional
> (Factory Method) y 3 de comportamiento (State, Observer, Strategy) para
> mostrar con más evidencia cómo SOLID y patrones se refuerzan entre sí,
> manteniendo el alcance dentro de lo visto en los Módulos 1 y 2.

### 4.3 Modelado UML

Diagrama de clases completo (Mermaid, se renderiza nativamente en
GitHub): [`docs/diagrama-clases.md`](docs/diagrama-clases.md)

**Tabla de trazabilidad:**

| Clase en el diagrama | Archivo en el repositorio | Coincide en atributos/métodos clave |
|---|---|---|
| `Pedido` | `src/main/java/com/restaurant/modelo/Pedido.java` | Sí |
| `ItemPedido` | `src/main/java/com/restaurant/modelo/ItemPedido.java` | Sí |
| `Plato` | `src/main/java/com/restaurant/modelo/Plato.java` | Sí |
| `TipoPlato` | `src/main/java/com/restaurant/fabrica/TipoPlato.java` | Sí |
| `PlatoFactory` | `src/main/java/com/restaurant/fabrica/PlatoFactory.java` | Sí |
| `EstadoPedido` | `src/main/java/com/restaurant/estado/EstadoPedido.java` | Sí |
| `EstadoCreado` | `src/main/java/com/restaurant/estado/EstadoCreado.java` | Sí |
| `EstadoEnPreparacion` | `src/main/java/com/restaurant/estado/EstadoEnPreparacion.java` | Sí |
| `EstadoListo` | `src/main/java/com/restaurant/estado/EstadoListo.java` | Sí |
| `EstadoEntregado` | `src/main/java/com/restaurant/estado/EstadoEntregado.java` | Sí |
| `Notificador` | `src/main/java/com/restaurant/observador/Notificador.java` | Sí |
| `NotificadorCocina` | `src/main/java/com/restaurant/observador/NotificadorCocina.java` | Sí |
| `NotificadorMesero` | `src/main/java/com/restaurant/observador/NotificadorMesero.java` | Sí |
| `EstrategiaReporte` | `src/main/java/com/restaurant/reportes/EstrategiaReporte.java` | Sí |
| `ReportePlatosMasPedidos` | `src/main/java/com/restaurant/reportes/ReportePlatosMasPedidos.java` | Sí |
| `ReportePlatosMenosPedidos` | `src/main/java/com/restaurant/reportes/ReportePlatosMenosPedidos.java` | Sí |
| `ReporteTiempoPromedio` | `src/main/java/com/restaurant/reportes/ReporteTiempoPromedio.java` | Sí |
| `ReporteService` | `src/main/java/com/restaurant/reportes/ReporteService.java` | Sí |
| `GestorPedidos` | `src/main/java/com/restaurant/servicio/GestorPedidos.java` | Sí |

## 5. Implementación

**Estructura de paquetes:**

```
src/main/java/com/restaurant/
├── Main.java              # Demo ejecutable de consola
├── modelo/                # Entidades del dominio: Pedido, ItemPedido, Plato
├── fabrica/                # Factory Method: PlatoFactory, TipoPlato
├── estado/                 # Patrón State: EstadoPedido y sus 4 estados
├── observador/              # Patrón Observer: Notificador y sus implementaciones
├── reportes/                # Patrón Strategy: EstrategiaReporte y ReporteService
└── servicio/                # Orquestación: GestorPedidos
```

**Dónde se aplica cada patrón/principio (enlaces directos):**
- Factory Method → [`PlatoFactory.java`](src/main/java/com/restaurant/fabrica/PlatoFactory.java)
- State → [`EstadoPedido.java`](src/main/java/com/restaurant/estado/EstadoPedido.java) y paquete `estado/`
- Observer → [`Notificador.java`](src/main/java/com/restaurant/observador/Notificador.java) y paquete `observador/`
- Strategy → [`EstrategiaReporte.java`](src/main/java/com/restaurant/reportes/EstrategiaReporte.java) y paquete `reportes/`
- SOLID (SRP/DIP/ISP) → ver comentarios Javadoc en cada clase citada en 4.1

**Instrucciones de ejecución:** ver [`README_TECNICO.md`](README_TECNICO.md).

## 6. Análisis Técnico

**Cohesión y acoplamiento (con ejemplos concretos):**
- Alta cohesión: `Pedido` solo mezcla datos y comportamiento que le
  pertenecen directamente a un pedido (sus ítems, su estado, sus
  timestamps); no contiene lógica de reportes ni de creación de platos.
- Bajo acoplamiento: `Pedido` y `GestorPedidos` dependen de las interfaces
  `EstadoPedido` y `Notificador`, no de clases concretas — se puede
  reemplazar `NotificadorCocina` por cualquier otra implementación sin
  recompilar `Pedido`.

**Extensiones futuras que el diseño facilita:**
- Agregar un nuevo estado (p. ej. `Cancelado`) o un nuevo canal de
  notificación (p. ej. notificación push) sin modificar clases existentes.
- Agregar un nuevo reporte (p. ej. "ventas por hora") implementando
  `EstrategiaReporte`.

**Límites honestos del diseño (lo que este diseño *no* resuelve todavía):**
- No hay persistencia: si el proceso termina, se pierden los pedidos. El
  diseño actual (`GestorPedidos` guardando una lista en memoria) tendría
  que evolucionar hacia un repositorio (interfaz `PedidoRepository`) para
  soportar una base de datos sin romper el resto del sistema — eso queda
  fuera del alcance de este corte.
- No hay concurrencia real (varios meseros tomando pedidos al mismo
  tiempo); la demo es secuencial.

## 7. Créditos y Roles

| Integrante | Rol / contribución principal |
|---|---|
| ... | ... |

---

### Recordatorio de entregables (según enunciado oficial)
- [ ] Repositorio en GitHub con código y documentación
- [ ] Este Wiki completo
- [ ] Presentación creativa del problema
- [ ] *(Opcional)* Video técnico explicando la solución (máx. 5 min)
