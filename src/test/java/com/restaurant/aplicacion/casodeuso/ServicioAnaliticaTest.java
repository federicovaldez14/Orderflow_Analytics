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

import java.time.LocalDateTime;
import java.util.List;

import static com.restaurant.soporte.Datos.BANDEJA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
    @DisplayName("Con periodo HOY solo cuenta el pedido de hoy")
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
}
