package com.restaurant.infraestructura.persistencia;

import com.restaurant.aplicacion.puerto.salida.InventarioRepositorio.Motivo;
import com.restaurant.dominio.inventario.Ingrediente;
import com.restaurant.dominio.inventario.MovimientoInventario;
import com.restaurant.dominio.inventario.Receta;
import com.restaurant.dominio.inventario.StockInsuficienteException;
import com.restaurant.dominio.inventario.TipoMovimiento;
import com.restaurant.dominio.inventario.UnidadMedida;
import com.restaurant.soporte.BaseDeDatosH2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * INTEGRACIÓN adaptador de inventario <-> H2 real.
 *
 * Además de lo básico, aquí vive la evidencia más importante del Reto 1:
 * con muchos hilos pidiendo el mismo ingrediente a la vez, el stock nunca
 * queda negativo y cada unidad vendida tiene exactamente un movimiento.
 */
class InventarioRepositorioJdbcIT {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 9, 28, 21, 0);

    private DataSource ds;
    private InventarioRepositorioJdbc repo;

    @BeforeEach
    void setUp() {
        ds = BaseDeDatosH2.nueva();
        repo = new InventarioRepositorioJdbc(ds);
        repo.initSchema();
        repo.guardarIngrediente(new Ingrediente("HUEVO", "Huevo", UnidadMedida.UNIDAD, 10, 4));
        repo.guardarIngrediente(new Ingrediente("ARROZ", "Arroz", UnidadMedida.GRAMO, 1_000, 200));
    }

    private static Motivo venta(int pedidoId) {
        return new Motivo(TipoMovimiento.SALIDA_VENTA, pedidoId, "Venta pedido #" + pedidoId, AHORA);
    }

    @Test
    @DisplayName("Given stock suficiente, When se descuenta, Then baja el stock y queda un movimiento por ingrediente")
    void shouldDiscountAndTrace() {
        // Act
        Map<String, Long> resultantes = repo.descontar(Map.of("HUEVO", 3L, "ARROZ", 300L), venta(1));

        // Assert
        assertEquals(7L, resultantes.get("HUEVO"));
        assertEquals(7L, repo.buscarIngrediente("HUEVO").orElseThrow().getStock());
        List<MovimientoInventario> movs = repo.movimientosDePedido(1);
        assertEquals(2, movs.size());
        assertEquals(TipoMovimiento.SALIDA_VENTA, movs.get(0).tipo());
        assertEquals(1, movs.get(0).pedidoId());
    }

    @Test
    @DisplayName("Given alcanza el arroz pero no el huevo, When se descuenta, Then NO se descuenta nada (atomicidad)")
    void shouldBeAllOrNothing() {
        StockInsuficienteException e = assertThrows(StockInsuficienteException.class,
                () -> repo.descontar(Map.of("HUEVO", 11L, "ARROZ", 100L), venta(2)));

        assertEquals("HUEVO", e.getFaltantes().get(0).codigo());
        assertEquals(10L, e.getFaltantes().get(0).disponible());
        assertEquals(10L, repo.buscarIngrediente("HUEVO").orElseThrow().getStock());
        assertEquals(1_000L, repo.buscarIngrediente("ARROZ").orElseThrow().getStock());
        assertTrue(repo.movimientos(null, 50).isEmpty());
    }

    @Test
    @DisplayName("Given un descuento del stock exacto (límite), When se descuenta, Then queda en 0")
    void shouldAllowDiscountToZero() {
        repo.descontar(Map.of("HUEVO", 10L), venta(3));
        assertEquals(0L, repo.buscarIngrediente("HUEVO").orElseThrow().getStock());
    }

    @Test
    @DisplayName("Sumar registra la entrada; registrar merma no cambia el stock")
    void shouldAddAndRegisterWaste() {
        repo.sumar(Map.of("ARROZ", 500L), new Motivo(TipoMovimiento.ENTRADA, null, "Compra", AHORA));
        repo.registrarSinCambio(Map.of("ARROZ", 150L), new Motivo(TipoMovimiento.MERMA, 9, "Merma", AHORA));

        assertEquals(1_500L, repo.buscarIngrediente("ARROZ").orElseThrow().getStock());
        List<MovimientoInventario> movs = repo.movimientos("ARROZ", 10);
        assertEquals(TipoMovimiento.MERMA, movs.get(0).tipo());      // más reciente primero
        assertEquals(1_500L, movs.get(0).stockResultante());
        assertEquals(TipoMovimiento.ENTRADA, movs.get(1).tipo());
        assertEquals(null, movs.get(1).pedidoId());
    }

    @Test
    @DisplayName("La base rechaza un stock negativo aunque alguien salte la regla (CHECK stock >= 0)")
    void shouldRejectNegativeStockAtDatabaseLevel() throws SQLException {
        try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
            assertThrows(SQLException.class,
                    () -> st.executeUpdate("UPDATE ingrediente SET stock = -1 WHERE codigo = 'HUEVO'"));
        }
    }

    @Test
    @DisplayName("Las recetas se guardan y se leen por plato")
    void shouldStoreRecipes() {
        repo.guardarReceta(new Receta("Huevos con arroz", Map.of("HUEVO", 2L, "ARROZ", 150L)));

        Receta leida = repo.recetasPorPlato().get("Huevos con arroz");

        assertEquals(2L, leida.getPorPorcion().get("HUEVO"));
        assertEquals(150L, leida.getPorPorcion().get("ARROZ"));
    }

    @Test
    @DisplayName("CONCURRENCIA: 40 pedidos simultáneos por 1 huevo con 10 en stock -> se venden exactamente 10")
    void shouldNeverOversellUnderConcurrency() throws Exception {
        // Arrange
        int hilos = 40;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger exitosos = new AtomicInteger();
        AtomicInteger rechazados = new AtomicInteger();
        List<Future<?>> tareas = new ArrayList<>();

        // Act: todos esperan la "largada" y salen al mismo tiempo
        for (int i = 0; i < hilos; i++) {
            final int pedido = 100 + i;
            tareas.add(pool.submit(() -> {
                largada.await();
                try {
                    repo.descontar(Map.of("HUEVO", 1L, "ARROZ", 10L), venta(pedido));
                    exitosos.incrementAndGet();
                } catch (StockInsuficienteException e) {
                    rechazados.incrementAndGet();
                }
                return null;
            }));
        }
        largada.countDown();
        for (Future<?> t : tareas) {
            t.get(30, TimeUnit.SECONDS);   // si hubiera interbloqueo, esto fallaría por tiempo
        }
        pool.shutdown();

        // Assert
        assertEquals(10, exitosos.get());
        assertEquals(30, rechazados.get());
        assertEquals(0L, repo.buscarIngrediente("HUEVO").orElseThrow().getStock());
        assertEquals(900L, repo.buscarIngrediente("ARROZ").orElseThrow().getStock());
        assertEquals(10, repo.movimientos("HUEVO", 1000).size());
    }
}
