package com.restaurant.observador;

import com.restaurant.modelo.Pedido;

/** Simula la app/buscapersonas del mesero: le avisa cuando el plato está listo. */
public class NotificadorMesero implements Notificador {

    @Override
    public void notificar(Pedido pedido, String mensaje) {
        System.out.println("[MESERO] Pedido #" + pedido.getId() + " (mesa " + pedido.getMesa() + "): " + mensaje);
    }
}
