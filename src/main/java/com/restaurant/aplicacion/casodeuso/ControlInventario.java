package com.restaurant.aplicacion.casodeuso;

import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;

import java.util.List;

/**
 * Lo que GestorPedidos necesita saber del inventario (ISP: solo dos
 * operaciones). Separarlo permite probar GestorPedidos con un doble de
 * prueba y deja el inventario como un componente aparte.
 */
public interface ControlInventario {

    /** Descuenta los ingredientes de platos nuevos; falla si no alcanzan. */
    void reservar(Pedido pedido, List<ItemPedido> nuevos);

    /** Devuelve o registra como merma los ingredientes de platos quitados. */
    void liberar(Pedido pedido, List<ItemPedido> quitados, String estadoAntes);

    /** Implementación nula: el sistema funciona sin llevar inventario. */
    ControlInventario SIN_INVENTARIO = new ControlInventario() {
        @Override
        public void reservar(Pedido pedido, List<ItemPedido> nuevos) {
        }

        @Override
        public void liberar(Pedido pedido, List<ItemPedido> quitados, String estadoAntes) {
        }
    };
}
