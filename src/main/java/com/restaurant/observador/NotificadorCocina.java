package com.restaurant.observador;

import com.restaurant.modelo.Pedido;

/** Simula la pantalla/impresora de cocina que recibe cada comanda nueva. */
public class NotificadorCocina implements Notificador {

    @Override
    public void notificar(Pedido pedido, String mensaje) {
        System.out.println("[COCINA] Pedido #" + pedido.getId() + " (mesa " + pedido.getMesa() + "): " + mensaje);
    }
}
