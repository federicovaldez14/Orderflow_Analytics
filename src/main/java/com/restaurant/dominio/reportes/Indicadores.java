package com.restaurant.dominio.reportes;

import com.restaurant.dominio.modelo.Pedido;

import java.time.Duration;
import java.util.List;

/**
 * Indicadores clave (KPI) del periodo, para la fila de tarjetas del panel.
 *
 * @param ventas            suma de pedidos ENTREGADOS ($)
 * @param ticketPromedio    ventas / pedidos entregados ($, 0 si no hay)
 * @param tiempoPromedioMin creado -> entregado, en minutos (0 si no hay)
 * @param tasaCancelacion   cancelados / pedidos cerrados (entregados + cancelados), 0..1
 */
public record Indicadores(long ventas, int pedidosEntregados, int pedidosEnCurso, int pedidosCancelados,
                          long ticketPromedio, double tiempoPromedioMin, double tasaCancelacion) {

    public static Indicadores de(List<Pedido> pedidos) {
        long ventas = 0;
        int entregados = 0;
        int enCurso = 0;
        int cancelados = 0;
        long segundos = 0;
        for (Pedido p : pedidos) {
            switch (p.getEstadoNombre()) {
                case "Entregado":
                    entregados++;
                    ventas += p.calcularTotal();
                    Duration d = p.tiempoDeAtencion();
                    if (d != null) {
                        segundos += d.getSeconds();
                    }
                    break;
                case "Cancelado":
                    cancelados++;
                    break;
                default:
                    enCurso++;
            }
        }
        long ticket = entregados == 0 ? 0 : Math.round(ventas / (double) entregados);
        double tiempo = entregados == 0 ? 0 : Math.round(segundos / (double) entregados / 6.0) / 10.0;
        int cerrados = entregados + cancelados;
        double tasa = cerrados == 0 ? 0 : cancelados / (double) cerrados;
        return new Indicadores(ventas, entregados, enCurso, cancelados, ticket, tiempo, tasa);
    }
}
