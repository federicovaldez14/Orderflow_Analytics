package com.restaurant.infraestructura.persistencia;

import com.restaurant.aplicacion.puerto.salida.InventarioRepositorio.Motivo;
import com.restaurant.dominio.fabrica.TipoPlato;
import com.restaurant.dominio.inventario.Ingrediente;
import com.restaurant.dominio.inventario.StockInsuficienteException;
import com.restaurant.dominio.inventario.TipoMovimiento;
import com.restaurant.dominio.inventario.UnidadMedida;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.modelo.Plato;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * INTEGRACIÓN de los adaptadores JDBC contra un PostgreSQL REAL (el mismo motor
 * que usan Docker Compose y el despliegue), levantado con Testcontainers.
 *
 * Demuestra que el adaptador es intercambiable (ADR-002): el mismo código que
 * corre sobre H2 en las demás pruebas funciona sobre PostgreSQL, incluida la
 * regla más importante del Reto 1 (no sobrevender con pedidos simultáneos).
 * Necesita Docker; en el pipeline corre en la etapa de pruebas de integración.
 */
@Testcontainers
class PostgresAdaptadoresIT {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.7-alpine");

    private static HikariDataSource ds;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-15T17:00:00Z"), ZoneId.of("America/Bogota"));

    private InventarioRepositorioJdbc inventario;
    private PedidoRepositorioJdbc pedidos;

    @BeforeAll
    static void conectar() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(POSTGRES.getJdbcUrl());
        cfg.setUsername(POSTGRES.getUsername());
        cfg.setPassword(POSTGRES.getPassword());
        cfg.setMaximumPoolSize(20);
        ds = new HikariDataSource(cfg);
    }

    @AfterAll
    static void cerrar() {
        ds.close();
    }

    @BeforeEach
    void limpiarYCrearEsquema() throws SQLException {
        try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
            st.execute("DROP TABLE IF EXISTS item_pedido, pedido, movimiento_inventario, receta, ingrediente CASCADE");
            st.execute("DROP SEQUENCE IF EXISTS seq_pedido");
        }
        inventario = new InventarioRepositorioJdbc(ds);
        inventario.initSchema();
        pedidos = new PedidoRepositorioJdbc(ds, RELOJ);
        pedidos.initSchema();
        inventario.guardarIngrediente(new Ingrediente("HUEVO", "Huevo", UnidadMedida.UNIDAD, 10, 4));
    }

    @Test
    @DisplayName("PostgreSQL: un pedido con sus líneas se guarda y se lee igual; los ids salen de la secuencia")
    void shouldSaveAndReadOrderOnPostgres() {
        // Arrange
        int id = pedidos.siguienteId();
        Plato bandeja = new Plato("Bandeja Paisa", TipoPlato.FUERTE, 32_000, 18);
        Pedido pedido = Pedido.reconstituir(id, 3, "Creado", List.of(new ItemPedido(bandeja, 2)),
                LocalDateTime.now(RELOJ), null, null, null, null, RELOJ);

        // Act
        pedidos.guardar(pedido);
        Pedido leido = pedidos.buscarPorId(id).orElseThrow();

        // Assert
        assertEquals(id + 1, pedidos.siguienteId());
        assertEquals(64_000L, leido.calcularTotal());
        assertEquals(3, leido.getMesa());
        assertTrue(pedidos.buscarActivoPorMesa(3).isPresent());
    }

    @Test
    @DisplayName("PostgreSQL: 40 hilos piden 1 huevo con 10 en stock -> exactamente 10 ventas y stock 0 (Reto 1)")
    void shouldNeverOversellUnderConcurrencyOnPostgres() throws Exception {
        // Arrange
        ExecutorService pool = Executors.newFixedThreadPool(40);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger rechazos = new AtomicInteger();
        List<Future<?>> tareas = new ArrayList<>();

        // Act
        for (int i = 0; i < 40; i++) {
            int pedidoId = i + 1;
            tareas.add(pool.submit(() -> {
                largada.await();
                try {
                    inventario.descontar(Map.of("HUEVO", 1L),
                            new Motivo(TipoMovimiento.SALIDA_VENTA, pedidoId, "Venta " + pedidoId, LocalDateTime.now(RELOJ)));
                } catch (StockInsuficienteException e) {
                    rechazos.incrementAndGet();
                }
                return null;
            }));
        }
        largada.countDown();
        for (Future<?> t : tareas) {
            t.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        // Assert
        assertEquals(30, rechazos.get());
        assertEquals(0L, inventario.buscarIngrediente("HUEVO").orElseThrow().getStock());
        assertEquals(10, inventario.movimientos("HUEVO", 100).size());
    }
}
