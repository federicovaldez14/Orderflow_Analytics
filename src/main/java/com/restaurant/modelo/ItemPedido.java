package com.restaurant.modelo;

/** Una línea dentro de un pedido: un plato y la cantidad solicitada. */
public class ItemPedido {

    private final Plato plato;
    private final int cantidad;

    public ItemPedido(Plato plato, int cantidad) {
        this.plato = plato;
        this.cantidad = cantidad;
    }

    public Plato getPlato() {
        return plato;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double subtotal() {
        return plato.getPrecio() * cantidad;
    }

    @Override
    public String toString() {
        return cantidad + "x " + plato.getNombre();
    }
}
