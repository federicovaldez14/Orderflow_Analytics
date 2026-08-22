package com.restaurant.servicio;

import com.restaurant.modelo.Pedido;
import com.restaurant.observador.Notificador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SRP: esta clase se encarga únicamente de administrar el ciclo de vida
 * de los pedidos (crear, avanzar de estado, listar). No sabe cómo se
 * arman los reportes (eso es de ReporteService) ni cómo se construyen
 * los platos (eso es de PlatoFactory).
 *
 * DIP: recibe la lista de observadores como abstracción (Notificador) y
 * se la asigna a cada pedido nuevo, sin depender de NotificadorCocina o
 * NotificadorMesero directamente.
 */
public class GestorPedidos {

    private final List<Pedido> pedidos = new ArrayList<>();
    private final List<Notificador> observadoresPorDefecto;
    private int siguienteId = 1;

    public GestorPedidos(List<Notificador> observadoresPorDefecto) {
        this.observadoresPorDefecto = observadoresPorDefecto;
    }

    public Pedido crearPedido(int mesa) {
        Pedido pedido = new Pedido(siguienteId++, mesa);
        for (Notificador observador : observadoresPorDefecto) {
            pedido.agregarObservador(observador);
        }
        pedidos.add(pedido);
        return pedido;
    }

    public void avanzarEstado(Pedido pedido) {
        pedido.avanzarEstado();
    }

    public List<Pedido> getPedidos() {
        return Collections.unmodifiableList(pedidos);
    }
}
