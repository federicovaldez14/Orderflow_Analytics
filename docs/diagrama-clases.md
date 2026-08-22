# Diagrama de clases — Sistema de Gestión de Pedidos de Restaurante

```mermaid
classDiagram
    class Pedido {
        -int id
        -int mesa
        -List~ItemPedido~ items
        -List~Notificador~ observadores
        -EstadoPedido estadoActual
        -LocalDateTime horaCreacion
        -LocalDateTime horaListo
        -LocalDateTime horaEntregado
        +agregarItem(ItemPedido)
        +agregarObservador(Notificador)
        +avanzarEstado()
        +cambiarEstado(EstadoPedido)
        +calcularTotal() double
        +tiempoDeAtencion() Duration
    }

    class ItemPedido {
        -Plato plato
        -int cantidad
        +subtotal() double
    }

    class Plato {
        -String nombre
        -TipoPlato tipo
        -double precio
        -int tiempoPreparacionMinutos
    }

    class TipoPlato {
        <<enumeration>>
        ENTRADA
        FUERTE
        BEBIDA
        POSTRE
    }

    class PlatoFactory {
        +crear(TipoPlato, String, double)$ Plato
    }

    class EstadoPedido {
        <<interface>>
        +getNombre() String
        +avanzar(Pedido)
    }
    class EstadoCreado
    class EstadoEnPreparacion
    class EstadoListo
    class EstadoEntregado

    class Notificador {
        <<interface>>
        +notificar(Pedido, String)
    }
    class NotificadorCocina
    class NotificadorMesero

    class EstrategiaReporte {
        <<interface>>
        +generar(List~Pedido~) String
    }
    class ReportePlatosMasPedidos
    class ReportePlatosMenosPedidos
    class ReporteTiempoPromedio
    class ReporteService {
        +generarReporte(EstrategiaReporte, List~Pedido~) String
    }

    class GestorPedidos {
        -List~Pedido~ pedidos
        -List~Notificador~ observadoresPorDefecto
        +crearPedido(int) Pedido
        +avanzarEstado(Pedido)
    }

    Pedido "1" o-- "many" ItemPedido : contiene
    ItemPedido --> Plato
    PlatoFactory ..> Plato : crea
    PlatoFactory --> TipoPlato

    EstadoCreado ..|> EstadoPedido
    EstadoEnPreparacion ..|> EstadoPedido
    EstadoListo ..|> EstadoPedido
    EstadoEntregado ..|> EstadoPedido
    Pedido --> EstadoPedido : estado actual

    NotificadorCocina ..|> Notificador
    NotificadorMesero ..|> Notificador
    Pedido --> "many" Notificador : notifica

    ReportePlatosMasPedidos ..|> EstrategiaReporte
    ReportePlatosMenosPedidos ..|> EstrategiaReporte
    ReporteTiempoPromedio ..|> EstrategiaReporte
    ReporteService --> EstrategiaReporte : usa

    GestorPedidos --> Pedido : administra
    GestorPedidos --> Notificador
```

> GitHub renderiza bloques ```mermaid``` de forma nativa en Markdown, así
> que este archivo se ve como diagrama directamente en el repositorio
> (Wiki o carpeta `docs/`), sin depender de un servicio externo de
> imágenes que pueda caducar.
