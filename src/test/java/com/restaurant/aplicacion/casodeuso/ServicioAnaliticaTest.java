package com.restaurant.aplicacion.casodeuso;

import com.restaurant.aplicacion.excepcion.RecursoNoEncontradoException;
import com.restaurant.aplicacion.puerto.salida.PedidoRepositorio;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.reportes.Periodo;
import com.restaurant.dominio.reportes.Reporte;
import com.restaurant.soporte.RelojFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import static com.restaurant.soporte.Datos.BANDEJA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioAnaliticaTest {

    private PedidoRepositorio pedidos;
    private ServicioAnalitica servicio;
    private RelojFalso reloj;

    @BeforeEach
    void setUp() {
        pedidos = mock(PedidoRepositorio.class);
        reloj = RelojFalso.el(2026, 9, 28, 21, 0);
        servicio = new ServicioAnalitica(pedidos, reloj);
        Pedido hoy = Pedido.reconstituir(1, 1, "Entregado", List.of(new ItemPedido(BANDEJA, 1)),
                LocalDateTime.of(2026, 9, 28, 12, 0), null, null, LocalDateTime.of(2026, 9, 28, 12, 20), null, reloj);
        Pedido haceUnMes = Pedido.reconstituir(2, 1, "Entregado", List.of(new ItemPedido(BANDEJA, 2)),
                LocalDateTime.of(2026, 8, 28, 12, 0), null, null, LocalDateTime.of(2026, 8, 28, 12, 30), null, reloj);
        when(pedidos.listarTodos()).thenReturn(List.of(hoy, haceUnMes));
        when(pedidos.listarCreadosDesde(LocalDateTime.of(2026, 9, 28, 0, 0))).thenReturn(List.of(hoy));
    }

    @Test
    @DisplayName("El panel trae los 6 reportes y los KPI del periodo pedido")
    void shouldBuildFullPanel() {
        ServicioAnalitica.PanelAnalitico panel = servicio.panel(Periodo.TODO);

        assertEquals(6, panel.reportes().size());
        assertEquals(2, panel.pedidosConsiderados());
        assertEquals(96_000L, panel.indicadores().ventas());
    }

    @Test
    @DisplayName("Con periodo HOY filtra en la base de datos desde las 00:00 y solo cuenta el pedido de hoy")
    void shouldFilterPanelByPeriod() {
        ServicioAnalitica.PanelAnalitico panel = servicio.panel(Periodo.HOY);

        assertEquals(1, panel.pedidosConsiderados());
        assertEquals(32_000L, panel.indicadores().ventas());
    }

    @Test
    @DisplayName("Se puede pedir un reporte por su id; un id desconocido es 'no encontrado'")
    void shouldFindReportById() {
        Reporte r = servicio.reporte("platos-mas-pedidos", Periodo.TODO);
        assertEquals(3.0, r.datos().get(0).valor(), 0.001);
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.reporte("no-existe", Periodo.TODO));
        assertThrows(IllegalArgumentException.class, () -> servicio.panel(null));
    }

    // ---------------- Mitigación del cuello de botella (caché) ----------------

    @Test
    @DisplayName("Con caché de 3 s, dos consultas seguidas leen el repositorio una sola vez")
    void shouldReuseCachedPanelWithinValidity() {
        // Arrange
        ServicioAnalitica conCache = new ServicioAnalitica(pedidos, reloj, Duration.ofSeconds(3));

        // Act
        conCache.panel(Periodo.TODO);
        reloj.avanzar(Duration.ofSeconds(2));
        conCache.panel(Periodo.TODO);

        // Assert
        verify(pedidos, times(1)).listarTodos();
    }

    @Test
    @DisplayName("Con caché de 3 s, al cumplirse la vigencia (límite exacto) se vuelve a calcular")
    void shouldRecalculateAfterValidity() {
        ServicioAnalitica conCache = new ServicioAnalitica(pedidos, reloj, Duration.ofSeconds(3));

        conCache.panel(Periodo.TODO);
        reloj.avanzar(Duration.ofSeconds(3));
        conCache.panel(Periodo.TODO);

        verify(pedidos, times(2)).listarTodos();
    }

    @Test
    @DisplayName("Sin caché (vigencia 0) cada consulta recalcula; vigencia negativa se rechaza")
    void shouldRecalculateWithoutCache() {
        servicio.panel(Periodo.TODO);
        servicio.panel(Periodo.TODO);

        verify(pedidos, times(2)).listarTodos();
        assertThrows(IllegalArgumentException.class,
                () -> new ServicioAnalitica(pedidos, reloj, Duration.ofSeconds(-1)));
    }

    @Test
    @DisplayName("CONCURRENCIA: 20 usuarios piden el panel a la vez -> se calcula una sola vez")
    void shouldComputeOnlyOnceUnderConcurrentRequests() throws Exception {
        // Arrange: repositorio lento hecho a mano (stub) que cuenta cuántas veces lo llaman
        AtomicInteger lecturas = new AtomicInteger();
        PedidoRepositorio lento = new PedidoRepositorio() {
            public int siguienteId() { return 0; }
            public void guardar(Pedido p) { }
            public Optional<Pedido> buscarPorId(int id) { return Optional.empty(); }
            public Optional<Pedido> buscarActivoPorMesa(int m) { return Optional.empty(); }
            public List<Pedido> listarCreadosDesde(LocalDateTime d) { return List.of(); }
            public List<Pedido> listarTodos() {
                lecturas.incrementAndGet();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return List.of();
            }
        };
        ServicioAnalitica conCache = new ServicioAnalitica(lento, reloj, Duration.ofSeconds(3));
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<ServicioAnalitica.PanelAnalitico>> resultados = new ArrayList<>();

        // Act
        for (int i = 0; i < 20; i++) {
            resultados.add(pool.submit(() -> {
                largada.await();
                return conCache.panel(Periodo.TODO);
            }));
        }
        largada.countDown();
        for (Future<ServicioAnalitica.PanelAnalitico> f : resultados) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        // Assert
        assertEquals(1, lecturas.get());
    }
}
