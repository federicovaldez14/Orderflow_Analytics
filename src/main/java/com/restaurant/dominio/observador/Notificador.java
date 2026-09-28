package com.restaurant.dominio.observador;

import com.restaurant.dominio.modelo.Pedido;

/**
 * PATRÓN DE COMPORTAMIENTO: Observer. PRINCIPIO ISP: un solo método.
 *
 * Corte 2 (hexagonal): esta interfaz es un PUERTO DE SALIDA definido por el
 * dominio. Las implementaciones (consola, pantalla Swing) son ADAPTADORES
 * que viven en infraestructura. La dependencia apunta hacia adentro: el
 * dominio no conoce ninguna implementación.
 */
public interface Notificador {
    void notificar(Pedido pedido, String mensaje);
}
