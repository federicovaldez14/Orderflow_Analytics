package com.restaurant.dominio.inventario;

/**
 * Regla de negocio: qué pasa con los ingredientes de platos que se quitan o
 * se cancelan.
 *
 *  - Si el pedido seguía en "Creado", la cocina no los había tocado:
 *    vuelven al inventario (DEVOLUCION).
 *  - Si ya estaba "En preparación" o "Listo", se consideran perdidos:
 *    se registran como MERMA y el stock no cambia.
 */
public final class PoliticaDevolucion {

    private PoliticaDevolucion() {
    }

    public static TipoMovimiento tipoPara(String estadoAntesDeQuitar) {
        if ("Creado".equals(estadoAntesDeQuitar)) {
            return TipoMovimiento.DEVOLUCION;
        }
        if ("En preparación".equals(estadoAntesDeQuitar) || "Listo".equals(estadoAntesDeQuitar)) {
            return TipoMovimiento.MERMA;
        }
        throw new IllegalArgumentException(
                "Un pedido en estado '" + estadoAntesDeQuitar + "' no puede devolver ingredientes");
    }
}
