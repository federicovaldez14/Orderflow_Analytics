package com.restaurant.dominio.reportes;

import com.restaurant.dominio.modelo.Pedido;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiempo promedio (minutos) de cada etapa del servicio y del total. Solo
 * cuenta pedidos que ya pasaron por esa etapa. Es la base para ver DÓNDE está
 * la demora: en cocina (En preparación -> Listo) o en el salón (Listo -> Entregado).
 */
public class ReporteTiempoPromedio implements EstrategiaReporte {

    @Override
    public Reporte generar(List<Pedido> pedidos) {
        List<Reporte.Dato> datos = new ArrayList<>();
        agregar(datos, "Espera a cocina", pedidos, Pedido::getHoraCreacion, Pedido::getHoraEnPreparacion);
        agregar(datos, "Preparación", pedidos, Pedido::getHoraEnPreparacion, Pedido::getHoraListo);
        agregar(datos, "Entrega a la mesa", pedidos, Pedido::getHoraListo, Pedido::getHoraEntregado);
        agregar(datos, "Total (creado → entregado)", pedidos, Pedido::getHoraCreacion, Pedido::getHoraEntregado);
        return new Reporte("tiempo-promedio", "Tiempo promedio de atención por etapa", "min", datos);
    }

    private static void agregar(List<Reporte.Dato> datos, String etapa, List<Pedido> pedidos,
                                java.util.function.Function<Pedido, LocalDateTime> desde,
                                java.util.function.Function<Pedido, LocalDateTime> hasta) {
        long sumaSegundos = 0;
        int n = 0;
        for (Pedido p : pedidos) {
            LocalDateTime a = desde.apply(p);
            LocalDateTime b = hasta.apply(p);
            if (a != null && b != null) {
                sumaSegundos += Duration.between(a, b).getSeconds();
                n++;
            }
        }
        if (n > 0) {
            double minutos = Math.round(sumaSegundos / (double) n / 6.0) / 10.0;   // 1 decimal
            datos.add(new Reporte.Dato(etapa, minutos));
        }
    }
}
