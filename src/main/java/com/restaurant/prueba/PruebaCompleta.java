package com.restaurant.prueba;

import com.restaurant.fabrica.PlatoFactory;
import com.restaurant.fabrica.TipoPlato;
import com.restaurant.modelo.ItemPedido;
import com.restaurant.modelo.Pedido;
import com.restaurant.modelo.Plato;
import com.restaurant.observador.Notificador;
import com.restaurant.observador.NotificadorCocina;
import com.restaurant.observador.NotificadorMesero;
import com.restaurant.reportes.EstrategiaReporte;
import com.restaurant.reportes.ReportePlatosMasPedidos;
import com.restaurant.reportes.ReportePlatosMenosPedidos;
import com.restaurant.reportes.ReporteService;
import com.restaurant.reportes.ReporteTiempoPromedio;
import com.restaurant.servicio.GestorPedidos;

import java.util.Arrays;
import java.util.List;

/**
 * PruebaCompleta ejercita TODA la funcionalidad del sistema en un solo
 * recorrido, incluyendo casos borde. Sirve como "guion de prueba manual"
 * para verificar que cada patrón y cada principio realmente funcionan,
 * y también como evidencia para el video/sustentación del proyecto.
 *
 * Qué cubre:
 *  1. Creación de platos de las 4 categorías (Factory Method).
 *  2. 4 pedidos distintos, con más de un ítem cada uno.
 *  3. Flujo completo de estados para dos pedidos (State + Observer).
 *  4. Caso borde: intentar avanzar un pedido que ya está "Entregado"
 *     (debe quedarse igual, sin lanzar error).
 *  5. Un pedido que se queda a medio camino (nunca llega a "Entregado")
 *     para probar que el reporte de tiempo promedio lo excluye bien.
 *  6. Cálculo de totales.
 *  7. Los 3 reportes (Strategy), incluyendo el caso de tener 0 pedidos
 *     entregados para ReporteTiempoPromedio (mensaje alternativo).
 */
public class PruebaCompleta {

    public static void main(String[] args) throws InterruptedException {
        List<Notificador> observadores = Arrays.asList(new NotificadorCocina(), new NotificadorMesero());
        GestorPedidos gestor = new GestorPedidos(observadores);

        // 1) Un plato de cada categoría (prueba PlatoFactory completo)
        Plato patacones = PlatoFactory.crear(TipoPlato.ENTRADA, "Patacones con hogao", 14000);
        Plato bandejaPaisa = PlatoFactory.crear(TipoPlato.FUERTE, "Bandeja Paisa", 32000);
        Plato ajiaco = PlatoFactory.crear(TipoPlato.FUERTE, "Ajiaco santafereño", 28000);
        Plato limonada = PlatoFactory.crear(TipoPlato.BEBIDA, "Limonada de coco", 9000);
        Plato gaseosa = PlatoFactory.crear(TipoPlato.BEBIDA, "Gaseosa", 5000);
        Plato flan = PlatoFactory.crear(TipoPlato.POSTRE, "Flan de café", 8000);

        // 2) Cuatro pedidos en mesas distintas
        Pedido pedido1 = gestor.crearPedido(5);
        pedido1.agregarItem(new ItemPedido(bandejaPaisa, 2));
        pedido1.agregarItem(new ItemPedido(limonada, 2));

        Pedido pedido2 = gestor.crearPedido(3);
        pedido2.agregarItem(new ItemPedido(patacones, 1));
        pedido2.agregarItem(new ItemPedido(bandejaPaisa, 1));
        pedido2.agregarItem(new ItemPedido(flan, 1));

        Pedido pedido3 = gestor.crearPedido(8);
        pedido3.agregarItem(new ItemPedido(ajiaco, 3));
        pedido3.agregarItem(new ItemPedido(gaseosa, 3));

        Pedido pedido4 = gestor.crearPedido(1); // este se queda "colgado" a propósito
        pedido4.agregarItem(new ItemPedido(gaseosa, 1));

        System.out.println("========== FLUJO COMPLETO: pedido 1 ==========");
        avanzarHastaEntregado(gestor, pedido1);
        System.out.println("Estado final pedido 1: " + pedido1.getEstadoNombre());

        System.out.println("\n========== FLUJO COMPLETO: pedido 2 ==========");
        avanzarHastaEntregado(gestor, pedido2);
        System.out.println("Estado final pedido 2: " + pedido2.getEstadoNombre());

        System.out.println("\n========== FLUJO COMPLETO: pedido 3 ==========");
        avanzarHastaEntregado(gestor, pedido3);
        System.out.println("Estado final pedido 3: " + pedido3.getEstadoNombre());

        System.out.println("\n========== CASO BORDE: avanzar un pedido ya Entregado ==========");
        String estadoAntes = pedido1.getEstadoNombre();
        gestor.avanzarEstado(pedido1); // no debería pasar nada
        String estadoDespues = pedido1.getEstadoNombre();
        System.out.println("Estado antes: " + estadoAntes + " | Estado después: " + estadoDespues
                + " -> " + (estadoAntes.equals(estadoDespues) ? "OK, se ignoró correctamente" : "ERROR"));

        System.out.println("\n========== CASO BORDE: pedido que se queda a medias ==========");
        gestor.avanzarEstado(pedido4); // pasa a "En preparación" y se queda ahí
        System.out.println("Pedido 4 se queda en estado: " + pedido4.getEstadoNombre()
                + " (nunca llega a Entregado, no debe contar en el promedio de tiempos)");

        System.out.println("\n========== TOTALES ==========");
        for (Pedido p : gestor.getPedidos()) {
            System.out.printf("Pedido #%d (mesa %d) - estado: %-14s - total: $%.0f%n",
                    p.getId(), p.getMesa(), p.getEstadoNombre(), p.calcularTotal());
        }

        System.out.println("\n========== REPORTES ==========");
        ReporteService reporteService = new ReporteService();
        EstrategiaReporte[] estrategias = {
                new ReportePlatosMasPedidos(),
                new ReportePlatosMenosPedidos(),
                new ReporteTiempoPromedio()
        };
        for (EstrategiaReporte estrategia : estrategias) {
            System.out.println(reporteService.generarReporte(estrategia, gestor.getPedidos()));
        }

        System.out.println("========== CASO BORDE: reporte con lista vacía ==========");
        System.out.println(reporteService.generarReporte(new ReporteTiempoPromedio(), List.of()));
    }

    private static void avanzarHastaEntregado(GestorPedidos gestor, Pedido pedido) throws InterruptedException {
        while (!pedido.getEstadoNombre().equals("Entregado")) {
            Thread.sleep(30);
            gestor.avanzarEstado(pedido);
        }
    }
}
