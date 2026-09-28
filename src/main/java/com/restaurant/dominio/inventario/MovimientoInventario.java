package com.restaurant.dominio.inventario;

import java.time.LocalDateTime;

/**
 * Registro inmutable de un cambio de inventario: qué, cuánto, por qué,
 * cuándo, con qué pedido y cómo quedó el stock. Con esta tabla se puede
 * reconstruir la historia de cualquier ingrediente.
 *
 * @param cantidad         siempre positiva; el signo lo da el tipo
 * @param stockResultante  stock del ingrediente justo después del movimiento
 * @param pedidoId         pedido que lo originó (null para entradas y ajustes)
 */
public record MovimientoInventario(long id, LocalDateTime fecha, String ingrediente, TipoMovimiento tipo,
                                   long cantidad, long stockResultante, Integer pedidoId, String descripcion) {
}
