package com.restaurant.reportes;

import com.restaurant.modelo.Pedido;

import java.util.List;
import java.util.Map;

/**
 * Reutiliza el conteo de ReportePlatosMasPedidos pero lo ordena ascendente.
 * Muestra cómo, gracias a Strategy, cada reporte es intercambiable sin
 * tocar el resto del sistema.
 */
public class ReportePlatosMenosPedidos implements EstrategiaReporte {

    @Override
    public String generar(List<Pedido> pedidos) {
        Map<String, Integer> conteo = ReportePlatosMasPedidos.contarUnidadesPorPlato(pedidos);

        StringBuilder sb = new StringBuilder("== Platos menos pedidos ==\n");
        conteo.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append(" unidades\n"));
        return sb.toString();
    }
}
