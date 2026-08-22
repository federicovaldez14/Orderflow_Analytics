package com.restaurant.observador;

import com.restaurant.modelo.Pedido;

/**
 * PATRÓN DE COMPORTAMIENTO: Observer.
 * PRINCIPIO ISP: interfaz pequeña y específica (un solo método), en vez de
 * una interfaz "gorda" tipo IServicioRestaurante con notificar(), reportar(),
 * facturar(), etc. Cada implementación solo depende de lo que realmente usa.
 *
 * Pedido (el "sujeto") notifica a todos sus observadores cada vez que
 * cambia de estado, sin saber si del otro lado hay una pantalla de cocina,
 * una app del mesero o un log. Eso es lo que permite, más adelante, añadir
 * un canal nuevo (p. ej. una notificación push) sin tocar Pedido.
 */
public interface Notificador {
    void notificar(Pedido pedido, String mensaje);
}
