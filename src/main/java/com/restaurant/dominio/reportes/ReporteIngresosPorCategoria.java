package com.restaurant.dominio.reportes;

import com.restaurant.dominio.fabrica.TipoPlato;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Ingresos ($) de pedidos entregados por categoría de plato, de mayor a menor. */
public class ReporteIngresosPorCategoria implements EstrategiaReporte {

    @Override
    public Reporte generar(List<Pedido> pedidos) {
        Map<TipoPlato, Long> ingresos = new EnumMap<>(TipoPlato.class);
        for (Pedido p : pedidos) {
            if (!"Entregado".equals(p.getEstadoNombre())) {
                continue;
            }
            for (ItemPedido item : p.getItems()) {
                ingresos.merge(item.getPlato().getTipo(), item.subtotal(), Long::sum);
            }
        }
        List<Reporte.Dato> datos = new ArrayList<>();
        ingresos.entrySet().stream()
                .sorted(Map.Entry.<TipoPlato, Long>comparingByValue().reversed())
                .forEach(e -> datos.add(new Reporte.Dato(nombre(e.getKey()), e.getValue())));
        return new Reporte("ingresos-por-categoria", "Ingresos por categoría", "$", datos);
    }

    private static String nombre(TipoPlato t) {
        switch (t) {
            case ENTRADA:
                return "Entradas";
            case FUERTE:
                return "Platos fuertes";
            case BEBIDA:
                return "Bebidas";
            default:
                return "Postres";
        }
    }
}
