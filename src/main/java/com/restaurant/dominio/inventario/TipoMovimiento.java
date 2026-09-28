package com.restaurant.dominio.inventario;

/**
 * Por qué cambió (o se registró) el inventario. Es la base de la
 * trazabilidad: cada gramo que entra o sale queda con su motivo.
 */
public enum TipoMovimiento {
    /** Compra o reposición de mercancía (+). */
    ENTRADA,
    /** Ingredientes usados por un plato vendido (-). */
    SALIDA_VENTA,
    /** Se canceló o corrigió un pedido que la cocina aún no empezaba: vuelve al stock (+). */
    DEVOLUCION,
    /** Se canceló un pedido ya en preparación: el insumo se perdió. No cambia el stock
     *  (ya se había descontado), pero queda registrado como pérdida. */
    MERMA,
    /** Conteo físico que corrige el stock del sistema (+ o -). */
    AJUSTE
}
