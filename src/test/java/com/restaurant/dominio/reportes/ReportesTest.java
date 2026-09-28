package com.restaurant.dominio.reportes;

import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.soporte.RelojFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.restaurant.soporte.Datos.AJIACO;
import static com.restaurant.soporte.Datos.BANDEJA;
import static com.restaurant.soporte.Datos.FLAN;
import static com.restaurant.soporte.Datos.GASEOSA;
import static com.restaurant.soporte.Datos.LIMONADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reto 3 — las estrategias de reporte devuelven DATOS correctos.
 *
 * Escenario fijo (reloj falso, 28/09/2026):
 *  #1 12:10 mesa 1  2x Bandeja + 2x Limonada  -> Entregado (15 min)   82.000
 *  #2 13:30 mesa 2  1x Ajiaco + 3x Gaseosa    -> Entregado (25 min)   43.000
 *  #3 13:45 mesa 3  4x Gaseosa                -> Cancelado
 *  #4 19:05 mesa 4  1x Bandeja + 1x Flan      -> En preparación
 */
class ReportesTest {

    private List<Pedido> pedidos;

    @BeforeEach
    void setUp() {
        pedidos = new ArrayList<>();
        pedidos.add(entregado(1, 12, 10, 15, new ItemPedido(BANDEJA, 2), new ItemPedido(LIMONADA, 2)));
        pedidos.add(entregado(2, 13, 30, 25, new ItemPedido(AJIACO, 1), new ItemPedido(GASEOSA, 3)));
        Pedido cancelado = pedido(3, 13, 45, new ItemPedido(GASEOSA, 4));
        cancelado.cancelar();
        pedidos.add(cancelado);
        Pedido enCurso = pedido(4, 19, 5, new ItemPedido(BANDEJA, 1), new ItemPedido(FLAN, 1));
        enCurso.avanzarEstado();
        pedidos.add(enCurso);
    }

    @Test
    @DisplayName("Platos más pedidos: cuenta unidades, excluye cancelados y ordena de mayor a menor")
    void shouldRankDishesExcludingCancelled() {
        Reporte r = new ReportePlatosMasPedidos().generar(pedidos);

        assertEquals("Bandeja Paisa", r.datos().get(0).etiqueta());
        assertEquals(3.0, r.datos().get(0).valor(), 0.001);
        // Gaseosa: 3 del pedido entregado; las 4 del cancelado NO cuentan
        assertEquals(3.0, valor(r, "Gaseosa"), 0.001);
        assertEquals("unidades", r.unidad());
    }

    @Test
    @DisplayName("Platos menos pedidos: mismo conteo en orden ascendente (empates por nombre)")
    void shouldRankLeastOrdered() {
        Reporte r = new ReportePlatosMenosPedidos().generar(pedidos);

        assertEquals("Ajiaco santafereño", r.datos().get(0).etiqueta());
        assertEquals("Flan de café", r.datos().get(1).etiqueta());
    }

    @Test
    @DisplayName("Ventas por hora: solo entregados, rellena con 0 las horas sin ventas entre la primera y la última")
    void shouldSumSalesByHour() {
        Reporte r = new ReporteVentasPorHora().generar(pedidos);

        assertEquals(2, r.datos().size());                 // 12 h y 13 h (el de las 19 h no está entregado)
        assertEquals("12 h", r.datos().get(0).etiqueta());
        assertEquals(82_000, r.datos().get(0).valor(), 0.001);
        assertEquals(43_000, r.datos().get(1).valor(), 0.001);
    }

    @Test
    @DisplayName("Ingresos por categoría: fuertes 92.000, bebidas 33.000, ordenado de mayor a menor")
    void shouldSumRevenueByCategory() {
        Reporte r = new ReporteIngresosPorCategoria().generar(pedidos);

        assertEquals("Platos fuertes", r.datos().get(0).etiqueta());
        assertEquals(92_000, r.datos().get(0).valor(), 0.001);   // 64.000 + 28.000
        assertEquals(33_000, valor(r, "Bebidas"), 0.001);       // 18.000 + 15.000
    }

    @Test
    @DisplayName("Tiempo promedio: el total es el promedio de 15 y 25 min = 20 min")
    void shouldAverageServiceTime() {
        Reporte r = new ReporteTiempoPromedio().generar(pedidos);

        assertEquals(20.0, valor(r, "Total (creado → entregado)"), 0.001);
        assertEquals("min", r.unidad());
    }

    @Test
    @DisplayName("Pedidos por estado: uno por cada estado presente, en el orden del flujo")
    void shouldCountOrdersByState() {
        Reporte r = new ReportePedidosPorEstado().generar(pedidos);

        assertEquals("Creado", r.datos().get(0).etiqueta());
        assertEquals(2.0, valor(r, "Entregado"), 0.001);
        assertEquals(1.0, valor(r, "Cancelado"), 0.001);
        assertEquals(1.0, valor(r, "En preparación"), 0.001);
    }

    @Test
    @DisplayName("Sin pedidos, todos los reportes vienen vacíos (la UI muestra 'Sin datos')")
    void shouldReturnEmptyReportsWithoutOrders() {
        List<EstrategiaReporte> todas = List.of(new ReportePlatosMasPedidos(), new ReportePlatosMenosPedidos(),
                new ReporteVentasPorHora(), new ReporteIngresosPorCategoria(), new ReporteTiempoPromedio(),
                new ReportePedidosPorEstado());
        for (EstrategiaReporte e : todas) {
            assertTrue(new ReporteService().generarReporte(e, List.of()).vacio(), e.getClass().getSimpleName());
        }
    }

    @Test
    @DisplayName("Indicadores: ventas 125.000, ticket 62.500, 20 min, cancelación 1/3, 1 en curso")
    void shouldComputeKpis() {
        Indicadores k = Indicadores.de(pedidos);

        assertEquals(125_000L, k.ventas());
        assertEquals(2, k.pedidosEntregados());
        assertEquals(62_500L, k.ticketPromedio());
        assertEquals(20.0, k.tiempoPromedioMin(), 0.001);
        assertEquals(1 / 3.0, k.tasaCancelacion(), 0.0001);
        assertEquals(1, k.pedidosEnCurso());
    }

    @Test
    @DisplayName("Indicadores sin pedidos: todo en cero, sin dividir por cero")
    void shouldHandleNoOrdersInKpis() {
        Indicadores k = Indicadores.de(List.of());
        assertEquals(0L, k.ticketPromedio());
        assertEquals(0.0, k.tasaCancelacion(), 0.0);
    }

    @Test
    @DisplayName("Periodo: HOY incluye desde las 00:00, SEMANA los últimos 7 días (límite inclusivo)")
    void shouldFilterByPeriod() {
        RelojFalso reloj = RelojFalso.el(2026, 9, 28, 20, 0);
        LocalDateTime ahora = LocalDateTime.of(2026, 9, 28, 20, 0);
        Pedido ayer = Pedido.reconstituir(10, 1, "Entregado", List.of(new ItemPedido(GASEOSA, 1)),
                LocalDateTime.of(2026, 9, 27, 23, 59), null, null, null, null, reloj);
        Pedido hace6 = Pedido.reconstituir(11, 1, "Entregado", List.of(new ItemPedido(GASEOSA, 1)),
                LocalDateTime.of(2026, 9, 22, 0, 0), null, null, null, null, reloj);
        Pedido hace7 = Pedido.reconstituir(12, 1, "Entregado", List.of(new ItemPedido(GASEOSA, 1)),
                LocalDateTime.of(2026, 9, 21, 23, 59), null, null, null, null, reloj);
        List<Pedido> todos = new ArrayList<>(pedidos);
        todos.addAll(List.of(ayer, hace6, hace7));

        assertEquals(4, Periodo.HOY.filtrar(todos, ahora).size());
        assertEquals(6, Periodo.SEMANA.filtrar(todos, ahora).size());
        assertEquals(7, Periodo.TODO.filtrar(todos, ahora).size());
    }

    @Test
    @DisplayName("aTexto conserva la salida de consola de Corte 1")
    void shouldRenderAsText() {
        String texto = new ReportePlatosMasPedidos().generar(pedidos).aTexto();
        assertTrue(texto.startsWith("== Platos más pedidos =="));
        assertTrue(texto.contains("Bandeja Paisa: 3 unidades"));
    }

    // ------------------------------------------------------------------

    private static double valor(Reporte r, String etiqueta) {
        return r.datos().stream().filter(d -> d.etiqueta().equals(etiqueta)).findFirst().orElseThrow().valor();
    }

    private static Pedido pedido(int id, int hora, int minuto, ItemPedido... items) {
        RelojFalso reloj = RelojFalso.el(2026, 9, 28, hora, minuto);
        Pedido p = new Pedido(id, id, reloj);
        for (ItemPedido i : items) {
            p.agregarItem(i);
        }
        return p;
    }

    private static Pedido entregado(int id, int hora, int minuto, int minutosAtencion, ItemPedido... items) {
        RelojFalso reloj = RelojFalso.el(2026, 9, 28, hora, minuto);
        Pedido p = new Pedido(id, id, reloj);
        for (ItemPedido i : items) {
            p.agregarItem(i);
        }
        p.avanzarEstado();
        reloj.avanzarMinutos(minutosAtencion);
        p.avanzarEstado();
        p.avanzarEstado();
        return p;
    }
}
