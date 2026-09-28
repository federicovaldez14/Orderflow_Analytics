package com.restaurant.infraestructura.config;

import com.restaurant.aplicacion.puerto.salida.MenuRepositorio;
import com.restaurant.aplicacion.puerto.salida.PedidoRepositorio;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.modelo.Plato;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Historial de demostración: ~7 días de pedidos YA CERRADOS (entregados o
 * cancelados) para que la analítica tenga algo que graficar en la
 * presentación. Semilla fija: siempre genera los mismos datos.
 *
 * Se activa con orderflow.demo.historico=true (application.properties).
 * Las pruebas automáticas lo desactivan. Son datos "anteriores" al módulo de
 * inventario, por eso no generan movimientos de inventario.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "orderflow.demo.historico", havingValue = "true")
public class DatosHistoricosDemo implements ApplicationRunner {

    private final PedidoRepositorio pedidos;
    private final MenuRepositorio menu;
    private final Clock reloj;

    public DatosHistoricosDemo(PedidoRepositorio pedidos, MenuRepositorio menu, Clock reloj) {
        this.pedidos = pedidos;
        this.menu = menu;
        this.reloj = reloj;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!pedidos.listarTodos().isEmpty()) {
            return;
        }
        Random rnd = new Random(2026);
        List<Plato> carta = menu.listar();
        LocalDate hoy = LocalDate.now(reloj);
        // Horas con su peso: almuerzo y cena son las horas pico.
        int[] horas = {11, 12, 12, 12, 13, 13, 13, 13, 14, 14, 15, 18, 19, 19, 19, 20, 20, 21};
        for (int dia = 7; dia >= 1; dia--) {
            int pedidosDelDia = 18 + rnd.nextInt(14);
            for (int n = 0; n < pedidosDelDia; n++) {
                LocalDateTime creado = hoy.minusDays(dia)
                        .atTime(horas[rnd.nextInt(horas.length)], rnd.nextInt(60));
                pedidos.guardar(pedidoCerrado(creado, carta, rnd));
            }
        }
    }

    private Pedido pedidoCerrado(LocalDateTime creado, List<Plato> carta, Random rnd) {
        List<ItemPedido> items = new ArrayList<>();
        int lineas = 1 + rnd.nextInt(4);
        for (int i = 0; i < lineas; i++) {
            // Sesgo hacia los primeros platos fuertes: hay platos estrella y platos que casi no salen.
            Plato plato = carta.get(Math.min(carta.size() - 1, (int) Math.abs(rnd.nextGaussian() * carta.size() / 2.2)));
            items.add(new ItemPedido(plato, 1 + rnd.nextInt(3)));
        }
        LocalDateTime enPrep = creado.plusMinutes(1 + rnd.nextInt(6));
        LocalDateTime listo = enPrep.plusMinutes(8 + rnd.nextInt(18));
        LocalDateTime entregado = listo.plusMinutes(1 + rnd.nextInt(7));
        int mesa = 1 + rnd.nextInt(10);
        if (rnd.nextInt(100) < 7) {
            return Pedido.reconstituir(pedidos.siguienteId(), mesa, "Cancelado", items, creado, enPrep,
                    null, null, enPrep.plusMinutes(3), reloj);
        }
        return Pedido.reconstituir(pedidos.siguienteId(), mesa, "Entregado", items, creado, enPrep,
                listo, entregado, null, reloj);
    }
}
