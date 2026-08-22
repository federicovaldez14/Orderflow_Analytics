package com.restaurant.estado;

import com.restaurant.modelo.Pedido;

/** La cocina ya recibió y empezó a preparar la comanda. */
public class EstadoEnPreparacion implements EstadoPedido {

    @Override
    public String getNombre() {
        return "En preparación";
    }

    @Override
    public void avanzar(Pedido pedido) {
        pedido.cambiarEstado(new EstadoListo());
    }
}
