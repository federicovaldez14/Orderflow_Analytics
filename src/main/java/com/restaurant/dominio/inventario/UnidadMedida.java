package com.restaurant.dominio.inventario;

/**
 * Unidad base en la que se lleva el stock. Todo se guarda como número
 * entero en la unidad más pequeña (gramos, mililitros, unidades) para no
 * arrastrar errores de redondeo de double en sumas y restas.
 */
public enum UnidadMedida {
    GRAMO("g"),
    MILILITRO("ml"),
    UNIDAD("und");

    private final String simbolo;

    UnidadMedida(String simbolo) {
        this.simbolo = simbolo;
    }

    public String getSimbolo() {
        return simbolo;
    }
}
