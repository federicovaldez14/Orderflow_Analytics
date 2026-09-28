package com.restaurant.infraestructura.persistencia;

import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.soporte.BaseDeDatosH2;
import com.restaurant.soporte.RelojFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

import static com.restaurant.soporte.Datos.BANDEJA;
import static com.restaurant.soporte.Datos.GASEOSA;
import static com.restaurant.soporte.Datos.LIMONADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * INTEGRACIÓN en la frontera puerto de salida <-> adaptador <-> base de datos.
 * Usa H2 real (embebida), no mocks: se verifica que el SQL, los tipos y la
 * transacción funcionan de verdad.
 */
class PedidoRepositorioJdbcIT {

    private RelojFalso reloj;
    private PedidoRepositorioJdbc repo;

    @BeforeEach
    void setUp() {
        DataSource ds = BaseDeDatosH2.nueva();
        reloj = RelojFalso.el(2026, 9, 28, 19, 30);
        repo = new PedidoRepositorioJdbc(ds, reloj);
        repo.initSchema();
    }

    @Test
    @DisplayName("Given un pedido con dos líneas, When se guarda y se lee, Then se recupera igual")
    void shouldSaveAndReadOrder() {
        // Arrange
        Pedido p = new Pedido(repo.siguienteId(), 4, reloj);
        p.agregarItem(new ItemPedido(BANDEJA, 2));
        p.agregarItem(new ItemPedido(LIMONADA, 1));

        // Act
        repo.guardar(p);
        Pedido leido = repo.buscarPorId(p.getId()).orElseThrow();

        // Assert
        assertEquals(4, leido.getMesa());
        assertEquals("Creado", leido.getEstadoNombre());
        assertEquals(2, leido.getItems().size());
        assertEquals("Bandeja Paisa", leido.getItems().get(0).getPlato().getNombre());
        assertEquals(73_000L, leido.calcularTotal());
        assertEquals(p.getHoraCreacion(), leido.getHoraCreacion());
    }

    @Test
    @DisplayName("Given un pedido guardado, When avanza y se vuelve a guardar, Then se actualiza estado y hora")
    void shouldUpdateStateAndTimestamps() {
        Pedido p = new Pedido(repo.siguienteId(), 2, reloj);
        p.agregarItem(new ItemPedido(GASEOSA, 1));
        repo.guardar(p);

        reloj.avanzarMinutos(12);
        p.avanzarEstado();
        p.avanzarEstado();
        repo.guardar(p);

        Pedido leido = repo.buscarPorId(p.getId()).orElseThrow();
        assertEquals("Listo", leido.getEstadoNombre());
        assertNotNull(leido.getHoraListo());
        assertEquals(p.getHoraCreacion().plusMinutes(12), leido.getHoraListo());
    }

    @Test
    @DisplayName("Given se quitó una línea, When se guarda, Then la base refleja solo las líneas actuales")
    void shouldReplaceLines() {
        Pedido p = new Pedido(repo.siguienteId(), 2, reloj);
        p.agregarItem(new ItemPedido(BANDEJA, 1));
        p.agregarItem(new ItemPedido(GASEOSA, 1));
        repo.guardar(p);

        p.removerLinea(0);
        repo.guardar(p);

        List<ItemPedido> lineas = repo.buscarPorId(p.getId()).orElseThrow().getItems();
        assertEquals(1, lineas.size());
        assertEquals("Gaseosa", lineas.get(0).getPlato().getNombre());
    }

    @Test
    @DisplayName("Given un pedido activo y uno entregado en la misma mesa, When se busca el activo, Then solo aparece el activo")
    void shouldFindOnlyActiveOrderForTable() {
        Pedido viejo = new Pedido(repo.siguienteId(), 6, reloj);
        viejo.agregarItem(new ItemPedido(GASEOSA, 1));
        viejo.avanzarEstado();
        viejo.avanzarEstado();
        viejo.avanzarEstado();
        repo.guardar(viejo);
        Pedido actual = new Pedido(repo.siguienteId(), 6, reloj);
        actual.agregarItem(new ItemPedido(BANDEJA, 1));
        repo.guardar(actual);

        Optional<Pedido> activo = repo.buscarActivoPorMesa(6);

        assertTrue(activo.isPresent());
        assertEquals(actual.getId(), activo.get().getId());
        assertFalse(repo.buscarActivoPorMesa(7).isPresent());
    }

    @Test
    @DisplayName("Given varios pedidos, When se listan, Then vienen todos, en orden y con sus líneas")
    void shouldListAllWithLines() {
        for (int mesa = 1; mesa <= 3; mesa++) {
            Pedido p = new Pedido(repo.siguienteId(), mesa, reloj);
            p.agregarItem(new ItemPedido(GASEOSA, mesa));
            repo.guardar(p);
        }

        List<Pedido> todos = repo.listarTodos();

        assertEquals(3, todos.size());
        assertEquals(3, todos.get(2).getItems().get(0).getCantidad());
    }

    @Test
    @DisplayName("La secuencia nunca repite ids")
    void shouldNotRepeatIds() {
        assertNotEquals(repo.siguienteId(), repo.siguienteId());
    }
}
