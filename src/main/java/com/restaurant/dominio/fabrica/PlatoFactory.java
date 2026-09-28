package com.restaurant.dominio.fabrica;

import com.restaurant.dominio.modelo.Plato;

/**
 * PATRÓN CREACIONAL: Factory Method.
 *
 * Problema que resuelve: el sistema necesita crear objetos Plato de
 * distintas categorías sin que el código cliente conozca reglas de negocio
 * como "una bebida siempre tarda 2 minutos" o "un plato fuerte tarda 18".
 * Esas reglas quedan centralizadas aquí.
 *
 * Corte 2: el precio pasa de double a long (pesos colombianos, sin
 * centavos). Con double la división de cuenta no puede garantizar que las
 * partes sumen exactamente el total (0.1 + 0.2 != 0.3).
 */
public final class PlatoFactory {

    private PlatoFactory() {
    }

    public static Plato crear(TipoPlato tipo, String nombre, long precio) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de plato es obligatorio");
        }
        switch (tipo) {
            case ENTRADA:
                return new Plato(nombre, tipo, precio, 8);
            case FUERTE:
                return new Plato(nombre, tipo, precio, 18);
            case BEBIDA:
                return new Plato(nombre, tipo, precio, 2);
            case POSTRE:
                return new Plato(nombre, tipo, precio, 6);
            default:
                throw new IllegalArgumentException("Tipo de plato no soportado: " + tipo);
        }
    }
}
