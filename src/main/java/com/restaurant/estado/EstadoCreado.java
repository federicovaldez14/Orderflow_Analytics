package com.restaurant.estado;

import com.restaurant.modelo.Pedido;

/** Estado inicial: el mesero acaba de tomar la comanda. */
public class EstadoCreado implements EstadoPedido {

    @Override
    public String getNombre() {
        return "Creado";
    }

    @Override
    public void avanzar(Pedido pedido) {
        pedido.cambiarEstado(new EstadoEnPreparacion());
    }
}
