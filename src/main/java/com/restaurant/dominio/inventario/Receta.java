package com.restaurant.dominio.inventario;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cuánto de cada ingrediente lleva UNA porción de un plato.
 * Es lo que conecta la carta con el inventario: con la receta se sabe qué
 * descontar por cada plato vendido.
 */
public class Receta {

    private final String plato;
    private final Map<String, Long> porPorcion;

    public Receta(String plato, Map<String, Long> porPorcion) {
        if (plato == null || plato.isBlank()) {
            throw new IllegalArgumentException("La receta debe indicar el plato");
        }
        if (porPorcion == null || porPorcion.isEmpty()) {
            throw new IllegalArgumentException("La receta de '" + plato + "' debe tener al menos un ingrediente");
        }
        Map<String, Long> copia = new LinkedHashMap<>();
        porPorcion.forEach((codigo, cantidad) -> {
            if (cantidad == null || cantidad <= 0) {
                throw new IllegalArgumentException("Cantidad inválida de " + codigo + " en la receta de " + plato);
            }
            copia.put(codigo.trim().toUpperCase(), cantidad);
        });
        this.plato = plato.trim();
        this.porPorcion = Collections.unmodifiableMap(copia);
    }

    /** Consumo de ingredientes para N porciones. */
    public Map<String, Long> consumoPara(int porciones) {
        if (porciones <= 0) {
            throw new IllegalArgumentException("Las porciones deben ser positivas: " + porciones);
        }
        Map<String, Long> consumo = new LinkedHashMap<>();
        porPorcion.forEach((codigo, cantidad) -> consumo.put(codigo, cantidad * porciones));
        return consumo;
    }

    public String getPlato() {
        return plato;
    }

    public Map<String, Long> getPorPorcion() {
        return porPorcion;
    }
}
