package com.restaurant.reportes;

import com.restaurant.modelo.Pedido;

import java.time.Duration;
import java.util.List;

/**
 * Calcula el tiempo promedio de atención (creación -> entrega) usando los
 * timestamps que Pedido registra en cada cambio de estado. Es la base para
 * detectar cuellos de botella en el servicio.
 */
public class ReporteTiempoPromedio implements EstrategiaReporte {

    @Override
    public String generar(List<Pedido> pedidos) {
        List<Duration> tiempos = pedidos.stream()
                .map(Pedido::tiempoDeAtencion)
                .filter(d -> d != null)
                .collect(java.util.stream.Collectors.toList());

        if (tiempos.isEmpty()) {
            return "== Tiempo promedio de atención ==\nAún no hay pedidos entregados.\n";
        }

        long totalSegundos = tiempos.stream().mapToLong(Duration::getSeconds).sum();
        double promedioSegundos = totalSegundos / (double) tiempos.size();

        return String.format(
                "== Tiempo promedio de atención ==\n%d pedidos entregados, promedio: %.1f segundos\n",
                tiempos.size(), promedioSegundos);
    }
}
