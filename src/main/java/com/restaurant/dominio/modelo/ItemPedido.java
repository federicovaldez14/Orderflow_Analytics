package com.restaurant.dominio.modelo;

/** Una línea dentro de un pedido: un plato y la cantidad solicitada. */
public class ItemPedido {

    /** Límite por línea: evita errores de digitación (p. ej. 100 en vez de 10). */
    public static final int CANTIDAD_MAXIMA = 50;

    private final Plato plato;
    private final int cantidad;

    public ItemPedido(Plato plato, int cantidad) {
        if (plato == null) {
            throw new IllegalArgumentException("El plato es obligatorio");
        }
        if (cantidad < 1 || cantidad > CANTIDAD_MAXIMA) {
            throw new IllegalArgumentException(
                    "La cantidad debe estar entre 1 y " + CANTIDAD_MAXIMA + ": " + cantidad);
        }
        this.plato = plato;
        this.cantidad = cantidad;
    }

    public Plato getPlato() {
        return plato;
    }

    public int getCantidad() {
        return cantidad;
    }

    public long subtotal() {
        return plato.getPrecio() * cantidad;
    }

    @Override
    public String toString() {
        return cantidad + "x " + plato.getNombre();
    }
}
