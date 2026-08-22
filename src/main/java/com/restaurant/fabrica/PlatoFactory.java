package com.restaurant.fabrica;

import com.restaurant.modelo.Plato;

/**
 * PATRÓN CREACIONAL: Factory Method.
 *
 * Problema que resuelve: el mesero/el sistema necesita crear objetos Plato
 * de distintas categorías (entrada, fuerte, bebida, postre) sin que el
 * código cliente (GestorPedidos, Main) conozca reglas de negocio como
 * "una bebida siempre tarda 2 minutos" o "un plato fuerte tarda mínimo 15".
 * Esas reglas quedan centralizadas aquí.
 *
 * Por qué no se usó Builder: los platos no se arman por pasos opcionales
 * (no hay construcción incremental), son variantes de un mismo tipo de
 * objeto seleccionadas por una categoría — ese es exactamente el caso de
 * uso de Factory Method, no de Builder.
 */
public class PlatoFactory {

    public static Plato crear(TipoPlato tipo, String nombre, double precio) {
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
