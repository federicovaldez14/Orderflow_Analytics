package com.restaurant.dominio.reportes;

import com.restaurant.dominio.modelo.Pedido;

import java.util.ArrayList;
import java.util.List;

/**
 * Ventas ($) de pedidos ENTREGADOS según la hora en que se tomaron. Muestra
 * las horas pico del restaurante (para programar personal). Solo se listan
 * las horas desde la primera hasta la última con ventas, sin huecos: una
 * hora sin ventas aparece en 0, que también es información.
 */
public class ReporteVentasPorHora implements EstrategiaReporte {

    @Override
    public Reporte generar(List<Pedido> pedidos) {
        long[] porHora = new long[24];
        int primera = 24;
        int ultima = -1;
        for (Pedido p : pedidos) {
            if (!"Entregado".equals(p.getEstadoNombre())) {
                continue;
            }
            int h = p.getHoraCreacion().getHour();
            porHora[h] += p.calcularTotal();
            primera = Math.min(primera, h);
            ultima = Math.max(ultima, h);
        }
        List<Reporte.Dato> datos = new ArrayList<>();
        for (int h = primera; h <= ultima; h++) {
            datos.add(new Reporte.Dato(String.format("%02d h", h), porHora[h]));
        }
        return new Reporte("ventas-por-hora", "Ventas por hora del día", "$", datos);
    }
}
