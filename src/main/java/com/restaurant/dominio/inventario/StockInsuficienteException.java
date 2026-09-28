package com.restaurant.dominio.inventario;

import java.util.List;
import java.util.stream.Collectors;

/**
 * No alcanza el inventario para lo que se pidió. Lleva el detalle de TODOS
 * los ingredientes que faltan (no solo el primero) para que el mesero sepa
 * qué ofrecer en su lugar.
 */
public class StockInsuficienteException extends RuntimeException {

    public record Faltante(String codigo, String nombre, long requerido, long disponible, UnidadMedida unidad) {
        @Override
        public String toString() {
            return nombre + ": se necesitan " + requerido + " " + unidad.getSimbolo()
                    + " y hay " + disponible + " " + unidad.getSimbolo();
        }
    }

    private final List<Faltante> faltantes;

    public StockInsuficienteException(List<Faltante> faltantes) {
        super("Stock insuficiente -> " + faltantes.stream().map(Faltante::toString)
                .collect(Collectors.joining("; ")));
        this.faltantes = List.copyOf(faltantes);
    }

    public List<Faltante> getFaltantes() {
        return faltantes;
    }
}
