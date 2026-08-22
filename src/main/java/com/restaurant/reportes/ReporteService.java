package com.restaurant.reportes;

import com.restaurant.modelo.Pedido;

import java.util.List;

/**
 * DIP: ReporteService depende de la abstracción EstrategiaReporte, no de
 * clases concretas. Para agregar un reporte nuevo no se toca esta clase,
 * solo se le pasa una estrategia nueva desde el código cliente (Main).
 */
public class ReporteService {

    public String generarReporte(EstrategiaReporte estrategia, List<Pedido> pedidos) {
        return estrategia.generar(pedidos);
    }
}
