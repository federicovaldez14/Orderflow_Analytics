package com.restaurant.estado;

import com.restaurant.modelo.Pedido;

/** El plato está listo en la barra de cocina, esperando al mesero. */
public class EstadoListo implements EstadoPedido {

    @Override
    public String getNombre() {
        return "Listo";
    }

    @Override
    public void avanzar(Pedido pedido) {
        pedido.cambiarEstado(new EstadoEntregado());
    }
}
