package com.restaurant;

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
 * Demo de consola que recorre el flujo completo:
 * 1) Se crean pedidos con platos (Factory Method).
 * 2) Cada pedido avisa a cocina y mesero al cambiar de estado (Observer).
 * 3) El estado avanza Creado -> En preparación -> Listo -> Entregado (State).
 * 4) Al final se generan reportes de consumo (Strategy).
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        List<Notificador> observadores = Arrays.asList(new NotificadorCocina(), new NotificadorMesero());
        GestorPedidos gestor = new GestorPedidos(observadores);

        Plato bandejaPaisa = PlatoFactory.crear(TipoPlato.FUERTE, "Bandeja Paisa", 32000);
        Plato limonada = PlatoFactory.crear(TipoPlato.BEBIDA, "Limonada de coco", 9000);
        Plato patacones = PlatoFactory.crear(TipoPlato.ENTRADA, "Patacones con hogao", 14000);
        Plato flan = PlatoFactory.crear(TipoPlato.POSTRE, "Flan de café", 8000);

        Pedido pedido1 = gestor.crearPedido(5);
        pedido1.agregarItem(new ItemPedido(bandejaPaisa, 2));
        pedido1.agregarItem(new ItemPedido(limonada, 2));

        Pedido pedido2 = gestor.crearPedido(3);
        pedido2.agregarItem(new ItemPedido(patacones, 1));
        pedido2.agregarItem(new ItemPedido(bandejaPaisa, 1));
        pedido2.agregarItem(new ItemPedido(flan, 1));

        System.out.println("--- Avanzando pedido 1 por todos los estados ---");
        avanzarHastaEntregado(gestor, pedido1);

        System.out.println("\n--- Avanzando pedido 2 por todos los estados ---");
        avanzarHastaEntregado(gestor, pedido2);

        System.out.println("\n--- Totales ---");
        System.out.printf("Pedido #%d total: $%.0f%n", pedido1.getId(), pedido1.calcularTotal());
        System.out.printf("Pedido #%d total: $%.0f%n", pedido2.getId(), pedido2.calcularTotal());

        System.out.println("\n--- Reportes (Strategy) ---");
        ReporteService reporteService = new ReporteService();
        EstrategiaReporte[] estrategias = {
                new ReportePlatosMasPedidos(),
                new ReportePlatosMenosPedidos(),
                new ReporteTiempoPromedio()
        };
        for (EstrategiaReporte estrategia : estrategias) {
            System.out.println(reporteService.generarReporte(estrategia, gestor.getPedidos()));
        }
    }

    private static void avanzarHastaEntregado(GestorPedidos gestor, Pedido pedido) throws InterruptedException {
        while (!pedido.getEstadoNombre().equals("Entregado")) {
            Thread.sleep(50); // simula el paso del tiempo entre estados
            gestor.avanzarEstado(pedido);
        }
    }
}
