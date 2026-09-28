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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Caso de uso del RETO 3: analítica más completa y graficable.
 *
 * Lee los pedidos por el puerto, filtra por periodo y aplica todas las
 * estrategias de reporte (Strategy) + los indicadores clave. No sabe si el
 * resultado se dibuja en Swing o se entrega como JSON.
 *
 * CUELLO DE BOTELLA ENCONTRADO EN LA PRUEBA DE CARGA: calcular el panel
 * exige traer a memoria todos los pedidos del periodo con sus líneas (O(n)).
 * Con decenas de miles de pedidos y 30 usuarios consultando a la vez, cada
 * consulta cargaba su propia copia del historial y el servicio se cayó.
 * Mitigación aplicada (sin tocar el dominio):
 *   1. HOY y SEMANA se filtran en la base de datos (listarCreadosDesde).
 *   2. Caché corta por periodo ("vigencia", 3 s por defecto) con UN SOLO
 *      cálculo a la vez: si 30 usuarios piden el panel, uno lo calcula y los
 *      demás esperan y reciben ese mismo resultado. Un panel que se refresca
 *      cada 2 s en la UI tolera datos de hasta 3 s.
 * La solución de fondo es CQRS (módulo 5): un modelo de lectura con totales
 * precalculados detrás de un puerto. Ver docs/arquitectura.md §7.
 */
public class ServicioAnalitica {

    /** Todo lo que muestra el panel de analítica en una sola lectura. */
    public record PanelAnalitico(Periodo periodo, int pedidosConsiderados, Indicadores indicadores,
                                 List<Reporte> reportes) {
    }

    private record EnCache(PanelAnalitico panel, Instant calculadoEn) {
    }

    private final PedidoRepositorio pedidos;
    private final Clock reloj;
    private final Duration vigencia;
    private final ReporteService reporteService = new ReporteService();
    private final List<EstrategiaReporte> estrategias = List.of(
            new ReporteVentasPorHora(),
            new ReportePlatosMasPedidos(),
            new ReporteIngresosPorCategoria(),
            new ReporteTiempoPromedio(),
            new ReportePedidosPorEstado(),
            new ReportePlatosMenosPedidos());

    /** Un candado y una entrada de caché por periodo (HOY, SEMANA, TODO). */
    private final Map<Periodo, Object> candados = new EnumMap<>(Periodo.class);
    private final Map<Periodo, EnCache> cache = new EnumMap<>(Periodo.class);

    /** Sin caché: cada consulta recalcula (útil en pruebas y para medir el "antes"). */
    public ServicioAnalitica(PedidoRepositorio pedidos, Clock reloj) {
        this(pedidos, reloj, Duration.ZERO);
    }

    public ServicioAnalitica(PedidoRepositorio pedidos, Clock reloj, Duration vigencia) {
        if (vigencia == null || vigencia.isNegative()) {
            throw new IllegalArgumentException("La vigencia de la caché no puede ser negativa");
        }
        this.pedidos = pedidos;
        this.reloj = reloj;
        this.vigencia = vigencia;
        for (Periodo p : Periodo.values()) {
            candados.put(p, new Object());
        }
    }

    public PanelAnalitico panel(Periodo periodo) {
        if (periodo == null) {
            throw new IllegalArgumentException("El periodo es obligatorio");
        }
        if (vigencia.isZero()) {
            return calcular(periodo);
        }
        synchronized (candados.get(periodo)) {
            EnCache actual = cache.get(periodo);
            Instant ahora = reloj.instant();
            if (actual != null && ahora.isBefore(actual.calculadoEn().plus(vigencia))) {
                return actual.panel();
            }
            PanelAnalitico nuevo = calcular(periodo);
            cache.put(periodo, new EnCache(nuevo, ahora));
            return nuevo;
        }
    }

    public Reporte reporte(String id, Periodo periodo) {
        for (Reporte r : panel(periodo).reportes()) {
            if (r.id().equals(id)) {
                return r;
            }
        }
        throw new RecursoNoEncontradoException("No existe el reporte '" + id + "'");
    }

    private PanelAnalitico calcular(Periodo periodo) {
        List<Pedido> filtrados = filtrar(periodo);
        List<Reporte> reportes = new ArrayList<>();
        for (EstrategiaReporte e : estrategias) {
            reportes.add(reporteService.generarReporte(e, filtrados));
        }
        return new PanelAnalitico(periodo, filtrados.size(), Indicadores.de(filtrados), reportes);
    }

    private List<Pedido> filtrar(Periodo periodo) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Optional<LocalDateTime> desde = periodo.desde(ahora);
        return desde.isPresent() ? pedidos.listarCreadosDesde(desde.get()) : pedidos.listarTodos();
    }
}
