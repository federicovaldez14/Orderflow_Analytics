package com.restaurant.estado;

import com.restaurant.modelo.Pedido;

/**
 * PATRÓN DE COMPORTAMIENTO: State.
 *
 * Cada estado del flujo Creado -> En preparación -> Listo -> Entregado
 * implementa esta interfaz y sabe cuál es el siguiente estado válido.
 * Pedido delega en el estado actual en lugar de tener un campo String y
 * un bloque if/else gigante (ver comparación antes/después en el README,
 * sección 4.1).
 */
public interface EstadoPedido {

    /** Nombre legible del estado, usado en reportes y notificaciones. */
    String getNombre();

    /**
     * Avanza el pedido al siguiente estado del flujo.
     * Si el estado actual es terminal (Entregado), no hace nada.
     */
    void avanzar(Pedido pedido);
}
