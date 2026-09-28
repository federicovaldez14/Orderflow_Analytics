package com.restaurant.dominio.cuenta;

import java.util.List;

/**
 * Lo que paga una persona: su parte del consumo, su parte de la propina y
 * el detalle de qué le tocó.
 */
public record ParteCuenta(String persona, long consumo, long propina, List<String> detalle) {

    public ParteCuenta {
        detalle = List.copyOf(detalle);
    }

    public long total() {
        return consumo + propina;
    }
}
