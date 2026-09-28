package com.restaurant.dominio.estado;

/**
 * Traduce el nombre guardado en la base de datos al objeto de estado.
 *
 * Existe porque, con persistencia (Corte 2), un Pedido se reconstruye desde
 * una fila de la tabla y el estado viaja como texto. Es el UNICO lugar que
 * conoce la correspondencia nombre -> clase; agregar un estado nuevo implica
 * una linea aqui y la clase nueva, sin tocar Pedido.
 */
public final class EstadosPedido {

    private EstadosPedido() {
    }

    public static EstadoPedido desdeNombre(String nombre) {
        if (nombre == null) {
            throw new IllegalArgumentException("El estado no puede ser null");
        }
        switch (nombre) {
            case "Creado":
                return new EstadoCreado();
            case "En preparación":
                return new EstadoEnPreparacion();
            case "Listo":
                return new EstadoListo();
            case "Entregado":
                return new EstadoEntregado();
            case "Cancelado":
                return new EstadoCancelado();
            default:
                throw new IllegalArgumentException("Estado desconocido: " + nombre);
        }
    }
}
