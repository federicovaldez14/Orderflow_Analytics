package com.restaurant.aplicacion.casodeuso;

import com.restaurant.aplicacion.excepcion.RecursoNoEncontradoException;
import com.restaurant.aplicacion.puerto.salida.PedidoRepositorio;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.reportes.EstrategiaReporte;
import com.restaurant.dominio.reportes.Indicadores;
import com.restaurant.dominio.reportes.Periodo;
import com.restaurant.dominio.reportes.Reporte;
import com.restaurant.dominio.reportes.ReporteIngresosPorCategoria;
import com.restaurant.dominio.reportes.ReportePedidosPorEstado;
import com.restaurant.dominio.reportes.ReportePlatosMasPedidos;
import com.restaurant.dominio.reportes.ReportePlatosMenosPedidos;
import com.restaurant.dominio.reportes.ReporteService;
import com.restaurant.dominio.reportes.ReporteTiempoPromedio;
import com.restaurant.dominio.reportes.ReporteVentasPorHora;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso del RETO 3: analítica más completa y graficable.
 *
 * Lee los pedidos por el puerto, filtra por periodo y aplica todas las
 * estrategias de reporte (Strategy) + los indicadores clave. No sabe si el
 * resultado se dibuja en Swing o se entrega como JSON.
 *
 * Límite conocido (se mide en la prueba de carga): calcula todo sobre la
 * lista completa de pedidos en cada consulta, O(n). Con historial grande la
 * mitigación natural es CQRS (módulo 5): un modelo de lectura con totales
 * precalculados. Ver docs/arquitectura.md.
 */
public class ServicioAnalitica {

    /** Todo lo que muestra el panel de analítica en una sola lectura. */
    public record PanelAnalitico(Periodo periodo, int pedidosConsiderados, Indicadores indicadores,
                                 List<Reporte> reportes) {
    }

    private final PedidoRepositorio pedidos;
    private final Clock reloj;
    private final ReporteService reporteService = new ReporteService();
    private final List<EstrategiaReporte> estrategias = List.of(
            new ReporteVentasPorHora(),
            new ReportePlatosMasPedidos(),
            new ReporteIngresosPorCategoria(),
            new ReporteTiempoPromedio(),
            new ReportePedidosPorEstado(),
            new ReportePlatosMenosPedidos());

    public ServicioAnalitica(PedidoRepositorio pedidos, Clock reloj) {
        this.pedidos = pedidos;
        this.reloj = reloj;
    }

    public PanelAnalitico panel(Periodo periodo) {
        List<Pedido> filtrados = filtrar(periodo);
        List<Reporte> reportes = new ArrayList<>();
        for (EstrategiaReporte e : estrategias) {
            reportes.add(reporteService.generarReporte(e, filtrados));
        }
        return new PanelAnalitico(periodo, filtrados.size(), Indicadores.de(filtrados), reportes);
    }

    public Reporte reporte(String id, Periodo periodo) {
        List<Pedido> filtrados = filtrar(periodo);
        for (EstrategiaReporte e : estrategias) {
            Reporte r = reporteService.generarReporte(e, filtrados);
            if (r.id().equals(id)) {
                return r;
            }
        }
        throw new RecursoNoEncontradoException("No existe el reporte '" + id + "'");
    }

    private List<Pedido> filtrar(Periodo periodo) {
        if (periodo == null) {
            throw new IllegalArgumentException("El periodo es obligatorio");
        }
        return periodo.filtrar(pedidos.listarTodos(), LocalDateTime.now(reloj));
    }
}
