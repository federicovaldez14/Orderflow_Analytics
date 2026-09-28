# Diagrama de clases del núcleo — Corte 2

Paquetes `dominio` y `aplicacion` (el núcleo de la hexagonal). Los adaptadores de
`infraestructura` implementan las interfaces marcadas como `<<puerto>>`.

```mermaid
classDiagram
    direction LR

    class Pedido {
        -int id
        -int mesa
        -EstadoPedido estadoActual
        -List~ItemPedido~ items
        -LocalDateTime horaCreacion, horaEnPreparacion, horaListo, horaEntregado, horaCancelado
        +agregarItem(ItemPedido)
        +removerLinea(int) ItemPedido
        +avanzarEstado()
        +cancelar()
        +calcularTotal() long
        +tiempoDeAtencion() Duration
        +reconstituir(...)$ Pedido
    }
    class ItemPedido {
        -Plato plato
        -int cantidad
        +subtotal() long
    }
    class Plato {
        -String nombre
        -TipoPlato tipo
        -long precio
        -int tiempoPreparacionMinutos
    }
    class PlatoFactory {
        +crear(TipoPlato, String, long)$ Plato
    }
    class EstadoPedido {
        <<interface>>
        +getNombre() String
        +avanzar(Pedido)
        +esTerminal() boolean
    }
    class Notificador {
        <<puerto>>
        +notificar(Pedido, String)
    }

    Pedido "1" o-- "*" ItemPedido
    ItemPedido --> Plato
    PlatoFactory ..> Plato : crea
    Pedido --> EstadoPedido : State
    Pedido --> "*" Notificador : Observer
    EstadoPedido <|.. EstadoCreado
    EstadoPedido <|.. EstadoEnPreparacion
    EstadoPedido <|.. EstadoListo
    EstadoPedido <|.. EstadoEntregado
    EstadoPedido <|.. EstadoCancelado

    %% ---------------- Reto 1: inventario ----------------
    class Ingrediente {
        -String codigo
        -UnidadMedida unidad
        -long stock
        -long stockMinimo
        +descontar(long)
        +reponer(long)
        +bajoMinimo() boolean
    }
    class Receta {
        -String plato
        -Map porPorcion
        +consumoPara(int) Map
    }
    class CalculadoraConsumo {
        +calcular(List~ItemPedido~, Map)$ Map
        +porcionesDisponibles(Receta, Map)$ long
    }
    class PoliticaDevolucion {
        +tipoPara(String estadoAntes)$ TipoMovimiento
    }
    class MovimientoInventario {
        <<record>>
        fecha, ingrediente, tipo, cantidad, stockResultante, pedidoId
    }
    class TipoMovimiento {
        <<enumeration>>
        ENTRADA SALIDA_VENTA DEVOLUCION MERMA AJUSTE
    }
    CalculadoraConsumo ..> Receta
    CalculadoraConsumo ..> ItemPedido
    MovimientoInventario --> TipoMovimiento

    %% ---------------- Reto 2: división de cuenta ----------------
    class EstrategiaDivision {
        <<interface>>
        +nombre() String
        +dividir(Pedido) List~ParteCuenta~
    }
    class DivisorCuenta {
        +dividir(Pedido, EstrategiaDivision, int propina)$ DivisionCuenta
    }
    class Repartidor {
        +repartir(long, List~Long~)$ List~Long~
        +iguales(long, int)$ List~Long~
    }
    class DivisionCuenta {
        <<record>>
        pedidoId, metodo, subtotal, propina, partes
    }
    EstrategiaDivision <|.. DivisionIgualitaria
    EstrategiaDivision <|.. DivisionPorConsumo
    EstrategiaDivision <|.. DivisionPorPorcentaje
    DivisorCuenta --> EstrategiaDivision : Strategy
    DivisorCuenta ..> DivisionCuenta
    DivisionIgualitaria ..> Repartidor
    DivisionPorConsumo ..> Repartidor
    DivisionPorPorcentaje ..> Repartidor

    %% ---------------- Reto 3: analítica ----------------
    class EstrategiaReporte {
        <<interface>>
        +generar(List~Pedido~) Reporte
    }
    class Reporte {
        <<record>>
        id, titulo, unidad, datos
    }
    class Indicadores {
        <<record>>
        ventas, ticketPromedio, tiempoPromedioMin, tasaCancelacion ...
    }
    class Periodo {
        <<enumeration>>
        HOY SEMANA TODO
    }
    EstrategiaReporte <|.. ReportePlatosMasPedidos
    EstrategiaReporte <|.. ReportePlatosMenosPedidos
    EstrategiaReporte <|.. ReporteTiempoPromedio
    EstrategiaReporte <|.. ReporteVentasPorHora
    EstrategiaReporte <|.. ReporteIngresosPorCategoria
    EstrategiaReporte <|.. ReportePedidosPorEstado
    EstrategiaReporte ..> Reporte

    %% ---------------- Aplicación: casos de uso y puertos ----------------
    class GestorPedidos {
        +crearPedido(int, List~LineaSolicitada~) Pedido
        +agregarItem()
        +quitarLinea()
        +avanzarEstado()
        +cancelar()
    }
    class ControlInventario {
        <<interface>>
        +reservar(Pedido, List)
        +liberar(Pedido, List, String)
    }
    class ServicioInventario {
        +reponer()
        +ajustarConteo()
        +movimientos()
        +porcionesDisponibles()
    }
    class ServicioCuenta {
        +dividir(int, EstrategiaDivision, int) DivisionCuenta
    }
    class ServicioAnalitica {
        +panel(Periodo) PanelAnalitico
    }
    class PedidoRepositorio {
        <<puerto>>
    }
    class MenuRepositorio {
        <<puerto>>
    }
    class InventarioRepositorio {
        <<puerto>>
        +descontar(Map, Motivo) Map
        +sumar()
        +registrarSinCambio()
    }
    class AlertaInventario {
        <<puerto>>
        +stockBajo(Ingrediente)
    }

    GestorPedidos --> PedidoRepositorio
    GestorPedidos --> MenuRepositorio
    GestorPedidos --> ControlInventario
    ControlInventario <|.. ServicioInventario
    ServicioInventario --> InventarioRepositorio
    ServicioInventario --> AlertaInventario
    ServicioInventario ..> CalculadoraConsumo
    ServicioInventario ..> PoliticaDevolucion
    ServicioCuenta --> PedidoRepositorio
    ServicioCuenta ..> DivisorCuenta
    ServicioAnalitica --> PedidoRepositorio
    ServicioAnalitica ..> EstrategiaReporte
    ServicioAnalitica ..> Indicadores
```
