package com.restaurant.fabrica;

/**
 * Categorías de plato que la cocina maneja.
 * Se usa como parámetro para PlatoFactory; añadir una categoría nueva
 * (p. ej. POSTRE) no obliga a tocar el código que ya construye pedidos.
 */
public enum TipoPlato {
    ENTRADA,
    FUERTE,
    BEBIDA,
    POSTRE
}
