package com.restaurant.modelo;

import com.restaurant.fabrica.TipoPlato;

/**
 * Representa un plato del menú. Es un objeto de datos simple (inmutable):
 * la responsabilidad de decidir CÓMO se crea un plato vive en PlatoFactory,
 * no aquí (Single Responsibility Principle).
 */
public class Plato {

    private final String nombre;
    private final TipoPlato tipo;
    private final double precio;
    private final int tiempoPreparacionMinutos;

    public Plato(String nombre, TipoPlato tipo, double precio, int tiempoPreparacionMinutos) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.precio = precio;
        this.tiempoPreparacionMinutos = tiempoPreparacionMinutos;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoPlato getTipo() {
        return tipo;
    }

    public double getPrecio() {
        return precio;
    }

    public int getTiempoPreparacionMinutos() {
        return tiempoPreparacionMinutos;
    }

    @Override
    public String toString() {
        return nombre + " (" + tipo + ") - $" + precio;
    }
}
