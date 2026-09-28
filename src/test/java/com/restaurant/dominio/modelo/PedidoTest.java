package com.restaurant.dominio.modelo;

import com.restaurant.dominio.observador.Notificador;
import com.restaurant.soporte.RelojFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static com.restaurant.soporte.Datos.BANDEJA;
import static com.restaurant.soporte.Datos.LIMONADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Reglas de negocio de Pedido (entidad del dominio). Sin base de datos ni
 * Spring: solo el objeto y un reloj falso. Cada prueba sigue AAA.
 */
class PedidoTest {

    private RelojFalso reloj;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        reloj = RelojFalso.el(2026, 9, 28, 12, 0);
        pedido = new Pedido(1, 5, reloj);
    }

    // ---------- Creación: clases de equivalencia y límites de id / mesa ----------

    @Test
    @DisplayName("Given id y mesa positivos, When se crea, Then queda en estado Creado y sin ítems")
    void shouldStartInCreadoState() {
        // Arrange + Act: el setUp ya creó el pedido
        // Assert
        assertEquals("Creado", pedido.getEstadoNombre());
        assertEquals(0, pedido.getItems().size());
        assertEquals(0L, pedido.calcularTotal());
        assertTrue(pedido.estaEditable());
    }

    @ParameterizedTest(name = "mesa = {0}")
    @ValueSource(ints = {0, -1})
    @DisplayName("Given una mesa cero o negativa, When se crea el pedido, Then se rechaza")
    void shouldRejectInvalidTable(int mesa) {
        assertThrows(IllegalArgumentException.class, () -> new Pedido(1, mesa, reloj));
    }

    @ParameterizedTest(name = "id = {0}")
    @ValueSource(ints = {0, -7})
    @DisplayName("Given un id cero o negativo, When se crea el pedido, Then se rechaza")
    void shouldRejectInvalidId(int id) {
        assertThrows(IllegalArgumentException.class, () -> new Pedido(id, 1, reloj));
    }

    @Test
    @DisplayName("Given la mesa 1 (límite inferior válido), When se crea, Then se acepta")
    void shouldAcceptTableOne() {
        Pedido p = new Pedido(2, 1, reloj);
        assertEquals(1, p.getMesa());
    }

    // ---------- Ítems ----------

    @Test
    @DisplayName("Given dos líneas, When se calcula el total, Then es la suma de subtotales")
    void shouldSumSubtotals() {
        // Arrange
        pedido.agregarItem(new ItemPedido(BANDEJA, 2));   // 64.000
        pedido.agregarItem(new ItemPedido(LIMONADA, 3));  // 27.000

        // Act
        long total = pedido.calcularTotal();

        // Assert
        assertEquals(91_000L, total);
    }

    @Test
    @DisplayName("Given un ítem null, When se agrega, Then se rechaza")
    void shouldRejectNullItem() {
        assertThrows(IllegalArgumentException.class, () -> pedido.agregarItem(null));
    }

    @Test
    @DisplayName("Given dos líneas, When se quita la primera, Then queda una y se devuelve la quitada")
    void shouldRemoveLine() {
        // Arrange
        pedido.agregarItem(new ItemPedido(BANDEJA, 1));
        pedido.agregarItem(new ItemPedido(LIMONADA, 1));

        // Act
        ItemPedido quitado = pedido.removerLinea(0);

        // Assert
        assertEquals(BANDEJA, quitado.getPlato());
        assertEquals(1, pedido.getItems().size());
        assertEquals(LIMONADA, pedido.getItems().get(0).getPlato());
    }

    @Test
    @DisplayName("Given una sola línea, When se intenta quitar, Then se rechaza (el pedido no puede quedar vacío)")
    void shouldNotRemoveLastLine() {
        pedido.agregarItem(new ItemPedido(BANDEJA, 1));
        assertThrows(IllegalStateException.class, () -> pedido.removerLinea(0));
        assertEquals(1, pedido.getItems().size());
    }

    @ParameterizedTest(name = "índice = {0}")
    @ValueSource(ints = {-1, 2})
    @DisplayName("Given un índice fuera de rango (límites -1 y tamaño), When se quita, Then se rechaza")
    void shouldRejectLineOutOfRange(int indice) {
        pedido.agregarItem(new ItemPedido(BANDEJA, 1));
        pedido.agregarItem(new ItemPedido(LIMONADA, 1));
        assertThrows(IllegalArgumentException.class, () -> pedido.removerLinea(indice));
    }

    // ---------- Flujo de estados (State) ----------

    @Test
    @DisplayName("Given un pedido nuevo, When avanza tres veces, Then recorre Creado -> En preparación -> Listo -> Entregado")
    void shouldFollowFullFlow() {
        pedido.avanzarEstado();
        assertEquals("En preparación", pedido.getEstadoNombre());
        pedido.avanzarEstado();
        assertEquals("Listo", pedido.getEstadoNombre());
        pedido.avanzarEstado();
        assertEquals("Entregado", pedido.getEstadoNombre());
        assertFalse(pedido.estaEditable());
    }

    @Test
    @DisplayName("Given un pedido Entregado, When se intenta avanzar, Then se queda Entregado (se ignora)")
    void shouldIgnoreAdvanceWhenDelivered() {
        entregar(pedido);
        pedido.avanzarEstado();
        assertEquals("Entregado", pedido.getEstadoNombre());
    }

    @Test
    @DisplayName("Given un pedido Entregado, When se agrega un ítem, Then se rechaza")
    void shouldNotEditDeliveredOrder() {
        pedido.agregarItem(new ItemPedido(BANDEJA, 1));
        entregar(pedido);
        assertThrows(IllegalStateException.class, () -> pedido.agregarItem(new ItemPedido(LIMONADA, 1)));
    }

    @Test
    @DisplayName("Given un pedido en preparación, When se cancela, Then queda Cancelado y con hora de cancelación")
    void shouldCancelOrder() {
        pedido.avanzarEstado();
        pedido.cancelar();
        assertEquals("Cancelado", pedido.getEstadoNombre());
        assertNotNull(pedido.getHoraCancelado());
        assertFalse(pedido.estaEditable());
    }

    @Test
    @DisplayName("Given un pedido Entregado, When se cancela, Then se rechaza")
    void shouldNotCancelDeliveredOrder() {
        entregar(pedido);
        assertThrows(IllegalStateException.class, () -> pedido.cancelar());
    }

    @Test
    @DisplayName("Given un pedido Cancelado, When se cancela de nuevo, Then se rechaza")
    void shouldNotCancelTwice() {
        pedido.cancelar();
        assertThrows(IllegalStateException.class, () -> pedido.cancelar());
    }

    // ---------- Tiempos (base de la analítica) ----------

    @Test
    @DisplayName("Given 10 min de preparación y 5 de entrega, When se entrega, Then el tiempo de atención es 15 min")
    void shouldMeasureServiceTime() {
        // Arrange
        pedido.avanzarEstado();          // En preparación a las 12:00
        reloj.avanzarMinutos(10);
        pedido.avanzarEstado();          // Listo a las 12:10
        reloj.avanzarMinutos(5);

        // Act
        pedido.avanzarEstado();          // Entregado a las 12:15

        // Assert
        assertEquals(Duration.ofMinutes(15), pedido.tiempoDeAtencion());
        assertEquals(pedido.getHoraCreacion().plusMinutes(10), pedido.getHoraListo());
    }

    @Test
    @DisplayName("Given un pedido sin entregar, When se pide el tiempo de atención, Then es null")
    void shouldHaveNoServiceTimeUntilDelivered() {
        pedido.avanzarEstado();
        assertNull(pedido.tiempoDeAtencion());
    }

    // ---------- Observer (doble de prueba) ----------

    @Test
    @DisplayName("Given dos observadores, When el pedido cambia de estado, Then ambos son notificados")
    void shouldNotifyAllObservers() {
        // Arrange
        Notificador cocina = mock(Notificador.class);
        Notificador mesero = mock(Notificador.class);
        pedido.agregarObservador(cocina);
        pedido.agregarObservador(mesero);

        // Act
        pedido.avanzarEstado();
        pedido.avanzarEstado();

        // Assert
        verify(cocina, times(2)).notificar(any(), anyString());
        verify(mesero, times(2)).notificar(any(), anyString());
    }

    // ---------- Reconstitución (lo que usa el repositorio) ----------

    @Test
    @DisplayName("Given datos guardados, When se reconstituye, Then conserva estado, ítems y horas sin notificar")
    void shouldRebuildFromStoredData() {
        Pedido leido = Pedido.reconstituir(9, 3, "Listo", java.util.List.of(new ItemPedido(BANDEJA, 2)),
                reloj.instant().atZone(reloj.getZone()).toLocalDateTime(), null, null, null, null, reloj);
        assertEquals(9, leido.getId());
        assertEquals("Listo", leido.getEstadoNombre());
        assertEquals(64_000L, leido.calcularTotal());
        leido.avanzarEstado();
        assertEquals("Entregado", leido.getEstadoNombre());
    }

    private static void entregar(Pedido p) {
        p.avanzarEstado();
        p.avanzarEstado();
        p.avanzarEstado();
    }
}
