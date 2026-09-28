package com.restaurant.dominio.reportes;

import com.restaurant.dominio.modelo.Pedido;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cuántos pedidos hay en cada estado, en el orden del flujo (Creado ... Cancelado). */
public class ReportePedidosPorEstado implements EstrategiaReporte {

    private static final List<String> ORDEN = List.of("Creado", "En preparación", "Listo", "Entregado", "Cancelado");

    @Override
    public Reporte generar(List<Pedido> pedidos) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        ORDEN.forEach(e -> conteo.put(e, 0));
        for (Pedido p : pedidos) {
            conteo.merge(p.getEstadoNombre(), 1, Integer::sum);
        }
        List<Reporte.Dato> datos = new ArrayList<>();
        if (!pedidos.isEmpty()) {
            conteo.forEach((estado, n) -> datos.add(new Reporte.Dato(estado, n)));
        }
        return new Reporte("pedidos-por-estado", "Pedidos por estado", "pedidos", datos);
    }
}
