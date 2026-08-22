package com.restaurant.estado;

import com.restaurant.modelo.Pedido;

/** Estado terminal: el mesero ya entregó el pedido en la mesa. */
public class EstadoEntregado implements EstadoPedido {

    @Override
    public String getNombre() {
        return "Entregado";
    }

    @Override
    public void avanzar(Pedido pedido) {
        // Estado terminal: no hay siguiente paso. No se lanza excepción
        // porque intentar "cerrar dos veces" un pedido no debería tumbar
        // el sistema, solo se ignora.
    }
}
