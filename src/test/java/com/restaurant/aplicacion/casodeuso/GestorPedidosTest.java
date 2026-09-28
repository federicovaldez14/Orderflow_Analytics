package com.restaurant.aplicacion.casodeuso;

import com.restaurant.aplicacion.excepcion.RecursoNoEncontradoException;
import com.restaurant.aplicacion.excepcion.ReglaNegocioException;
import com.restaurant.aplicacion.puerto.salida.MenuRepositorio;
import com.restaurant.aplicacion.puerto.salida.PedidoRepositorio;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.inventario.StockInsuficienteException;
import com.restaurant.dominio.inventario.UnidadMedida;
import com.restaurant.dominio.observador.Notificador;
import com.restaurant.soporte.RelojFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static com.restaurant.soporte.Datos.BANDEJA;
import static com.restaurant.soporte.Datos.LIMONADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Caso de uso GestorPedidos aislado con dobles de prueba (Mockito) para los
 * puertos de salida: igual que RegistryWithMockTest del taller, no hay base
 * de datos ni Spring; solo se verifica la orquestación y las reglas.
 */
class GestorPedidosTest {

    private static final int MESAS = 10;

    private PedidoRepositorio pedidos;
    private MenuRepositorio menu;
    private Notificador notificador;
    private ControlInventario inventario;
    private RelojFalso reloj;
    private GestorPedidos gestor;

    @BeforeEach
    void setUp() {
        pedidos = mock(PedidoRepositorio.class);
        menu = mock(MenuRepositorio.class);
        notificador = mock(Notificador.class);
        inventario = mock(ControlInventario.class);
        reloj = RelojFalso.el(2026, 9, 28, 13, 0);
        gestor = new GestorPedidos(pedidos, menu, inventario, List.of(notificador), MESAS, reloj);

        when(menu.buscarPorNombre("Bandeja Paisa")).thenReturn(Optional.of(BANDEJA));
        when(menu.buscarPorNombre("Limonada de coco")).thenReturn(Optional.of(LIMONADA));
        when(pedidos.siguienteId()).thenReturn(7);
    }

    // ---------- crearPedido ----------

    @Test
    @DisplayName("Given una mesa libre y platos de la carta, When se crea el pedido, Then se guarda y se notifica")
    void shouldCreateOrder() {
        // Arrange
        List<LineaSolicitada> lineas = List.of(new LineaSolicitada("Bandeja Paisa", 2),
                new LineaSolicitada("Limonada de coco", 1));

        // Act
        Pedido pedido = gestor.crearPedido(4, lineas);

        // Assert
        assertEquals(7, pedido.getId());
        assertEquals(4, pedido.getMesa());
        assertEquals(73_000L, pedido.calcularTotal());
        verify(pedidos).guardar(pedido);
        verify(notificador).notificar(any(), anyString());
    }

    @ParameterizedTest(name = "mesa = {0}")
    @ValueSource(ints = {0, 11, -3})
    @DisplayName("Given una mesa fuera de [1, 10], When se crea el pedido, Then se rechaza sin guardar")
    void shouldRejectTableOutOfRange(int mesa) {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.crearPedido(mesa, List.of(new LineaSolicitada("Bandeja Paisa", 1))));
        verify(pedidos, never()).guardar(any());
    }

    @ParameterizedTest(name = "mesa = {0}")
    @ValueSource(ints = {1, 10})
    @DisplayName("Given las mesas límite 1 y 10, When se crea el pedido, Then se acepta")
    void shouldAcceptBoundaryTables(int mesa) {
        Pedido p = gestor.crearPedido(mesa, List.of(new LineaSolicitada("Bandeja Paisa", 1)));
        assertEquals(mesa, p.getMesa());
    }

    @Test
    @DisplayName("Given una mesa con pedido activo, When se crea otro pedido, Then se rechaza (regla de negocio)")
    void shouldRejectOccupiedTable() {
        // Arrange
        Pedido existente = new Pedido(3, 4, reloj);
        when(pedidos.buscarActivoPorMesa(4)).thenReturn(Optional.of(existente));

        // Act + Assert
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> gestor.crearPedido(4, List.of(new LineaSolicitada("Bandeja Paisa", 1))));
        assertEquals(true, e.getMessage().contains("#3"));
        verify(pedidos, never()).guardar(any());
    }

    @Test
    @DisplayName("Given un plato que no está en la carta, When se crea el pedido, Then se responde 'no encontrado'")
    void shouldRejectUnknownDish() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> gestor.crearPedido(2, List.of(new LineaSolicitada("Pizza", 1))));
        verify(pedidos, never()).guardar(any());
    }

    @Test
    @DisplayName("Given una lista de platos vacía, When se crea el pedido, Then se rechaza")
    void shouldRejectEmptyOrder() {
        assertThrows(IllegalArgumentException.class, () -> gestor.crearPedido(2, List.of()));
    }

    // ---------- operaciones sobre un pedido existente ----------

    @Test
    @DisplayName("Given un pedido Creado, When se avanza, Then queda En preparación y se guarda")
    void shouldAdvanceOrder() {
        // Arrange
        Pedido p = pedidoCon(5, BANDEJA);
        when(pedidos.buscarPorId(5)).thenReturn(Optional.of(p));

        // Act
        Pedido resultado = gestor.avanzarEstado(5);

        // Assert
        assertEquals("En preparación", resultado.getEstadoNombre());
        verify(pedidos).guardar(p);
    }

    @Test
    @DisplayName("Given un pedido Entregado, When se intenta avanzar, Then se responde con regla de negocio")
    void shouldRejectAdvancingDeliveredOrder() {
        Pedido p = pedidoCon(5, BANDEJA);
        p.avanzarEstado();
        p.avanzarEstado();
        p.avanzarEstado();
        when(pedidos.buscarPorId(5)).thenReturn(Optional.of(p));

        assertThrows(ReglaNegocioException.class, () -> gestor.avanzarEstado(5));
        verify(pedidos, never()).guardar(any());
    }

    @Test
    @DisplayName("Given un id que no existe, When se consulta, Then se responde 'no encontrado'")
    void shouldFailWhenOrderDoesNotExist() {
        assertThrows(RecursoNoEncontradoException.class, () -> gestor.obtener(999));
    }

    @Test
    @DisplayName("Given un pedido activo, When se cancela, Then queda Cancelado y se guarda")
    void shouldCancelOrder() {
        Pedido p = pedidoCon(5, BANDEJA);
        when(pedidos.buscarPorId(5)).thenReturn(Optional.of(p));

        Pedido resultado = gestor.cancelar(5);

        assertEquals("Cancelado", resultado.getEstadoNombre());
        verify(pedidos).guardar(p);
    }

    @Test
    @DisplayName("Given un pedido ya Cancelado, When se cancela otra vez, Then se responde con regla de negocio")
    void shouldRejectCancellingTwice() {
        Pedido p = pedidoCon(5, BANDEJA);
        p.cancelar();
        when(pedidos.buscarPorId(5)).thenReturn(Optional.of(p));

        assertThrows(ReglaNegocioException.class, () -> gestor.cancelar(5));
    }

    @Test
    @DisplayName("Given un pedido activo, When se agrega un plato, Then el total aumenta y se guarda")
    void shouldAddItem() {
        Pedido p = pedidoCon(5, BANDEJA);
        when(pedidos.buscarPorId(5)).thenReturn(Optional.of(p));

        Pedido resultado = gestor.agregarItem(5, new LineaSolicitada("Limonada de coco", 2));

        assertEquals(32_000L + 18_000L, resultado.calcularTotal());
        verify(pedidos).guardar(p);
    }

    @Test
    @DisplayName("Given un pedido con dos líneas, When se quita una, Then queda una y se guarda")
    void shouldRemoveLine() {
        Pedido p = pedidoCon(5, BANDEJA);
        p.agregarItem(new ItemPedido(LIMONADA, 1));
        when(pedidos.buscarPorId(5)).thenReturn(Optional.of(p));

        Pedido resultado = gestor.quitarLinea(5, 1);

        assertEquals(1, resultado.getItems().size());
        verify(pedidos).guardar(p);
    }

    @Test
    @DisplayName("Given cero mesas, When se construye el gestor, Then se rechaza la configuración")
    void shouldRejectZeroTables() {
        assertThrows(IllegalArgumentException.class,
                () -> new GestorPedidos(pedidos, menu, inventario, List.of(), 0, reloj));
    }

    // ---------- integración con inventario (Reto 1) ----------

    @Test
    @DisplayName("Given un pedido nuevo, When se crea, Then primero se reserva inventario y luego se guarda")
    void shouldReserveInventoryWhenCreating() {
        Pedido p = gestor.crearPedido(1, List.of(new LineaSolicitada("Bandeja Paisa", 1)));

        verify(inventario).reservar(eq(p), anyList());
        verify(pedidos).guardar(p);
    }

    @Test
    @DisplayName("Given no alcanza el inventario, When se crea el pedido, Then no se guarda y se propaga el faltante")
    void shouldNotSaveWhenStockIsInsufficient() {
        // Arrange: el doble de inventario rechaza cualquier reserva
        StockInsuficienteException sinHuevos = new StockInsuficienteException(List.of(
                new StockInsuficienteException.Faltante("HUEVO", "Huevo", 1, 0, UnidadMedida.UNIDAD)));
        doThrow(sinHuevos).when(inventario).reservar(any(), anyList());

        // Act + Assert
        assertThrows(StockInsuficienteException.class,
                () -> gestor.crearPedido(1, List.of(new LineaSolicitada("Bandeja Paisa", 1))));
        verify(pedidos, never()).guardar(any());
    }

    @Test
    @DisplayName("Given falla el guardado, When se crea el pedido, Then se devuelve lo reservado (compensación)")
    void shouldCompensateWhenSaveFails() {
        doThrow(new IllegalStateException("BD caída")).when(pedidos).guardar(any());

        assertThrows(IllegalStateException.class,
                () -> gestor.crearPedido(1, List.of(new LineaSolicitada("Bandeja Paisa", 1))));
        verify(inventario).liberar(any(), anyList(), eq("Creado"));
    }

    @Test
    @DisplayName("Given un pedido en preparación, When se cancela, Then se liberan sus platos indicando el estado previo")
    void shouldReleaseInventoryWhenCancelling() {
        Pedido p = pedidoCon(5, BANDEJA);
        p.avanzarEstado();
        when(pedidos.buscarPorId(5)).thenReturn(Optional.of(p));

        gestor.cancelar(5);

        verify(inventario).liberar(eq(p), anyList(), eq("En preparación"));
    }

    @Test
    @DisplayName("Given un pedido Creado, When se quita una línea, Then se libera solo ese plato")
    void shouldReleaseOnlyRemovedLine() {
        Pedido p = pedidoCon(5, BANDEJA);
        p.agregarItem(new ItemPedido(LIMONADA, 1));
        when(pedidos.buscarPorId(5)).thenReturn(Optional.of(p));

        gestor.quitarLinea(5, 0);

        verify(inventario).liberar(eq(p), anyList(), eq("Creado"));
    }

    private Pedido pedidoCon(int id, com.restaurant.dominio.modelo.Plato plato) {
        Pedido p = new Pedido(id, 2, reloj);
        p.agregarItem(new ItemPedido(plato, 1));
        return p;
    }
}
