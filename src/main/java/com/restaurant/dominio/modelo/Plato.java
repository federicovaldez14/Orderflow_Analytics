package com.restaurant.dominio.modelo;

import com.restaurant.dominio.fabrica.TipoPlato;

import java.util.Objects;

/**
 * Representa un plato del menú. Es un objeto de valor inmutable: la
 * responsabilidad de decidir CÓMO se crea vive en PlatoFactory (SRP).
 *
 * Su identidad de negocio es el nombre: dos platos con el mismo nombre son
 * el mismo plato del menú (así se enlazan ítems de pedido y recetas).
 */
public class Plato {

    private final String nombre;
    private final TipoPlato tipo;
    private final long precio;
    private final int tiempoPreparacionMinutos;

    public Plato(String nombre, TipoPlato tipo, long precio, int tiempoPreparacionMinutos) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del plato es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo del plato es obligatorio");
        }
        if (precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que cero: " + precio);
        }
        this.nombre = nombre.trim();
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

    public long getPrecio() {
        return precio;
    }

    public int getTiempoPreparacionMinutos() {
        return tiempoPreparacionMinutos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Plato)) return false;
        return nombre.equals(((Plato) o).nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }

    @Override
    public String toString() {
        return nombre + " ($" + String.format("%,d", precio) + ")";
    }
}
