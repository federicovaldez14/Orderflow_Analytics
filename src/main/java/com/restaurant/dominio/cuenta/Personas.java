package com.restaurant.dominio.cuenta;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Reglas comunes sobre quiénes dividen la cuenta. */
final class Personas {

    /** Una mesa grande de restaurante; más allá es casi seguro un error de digitación. */
    static final int MAXIMO = 20;

    private Personas() {
    }

    static void validar(Collection<String> personas) {
        if (personas == null || personas.isEmpty()) {
            throw new IllegalArgumentException("Debe haber al menos una persona para dividir la cuenta");
        }
        if (personas.size() > MAXIMO) {
            throw new IllegalArgumentException("Máximo " + MAXIMO + " personas por cuenta: " + personas.size());
        }
        Set<String> vistas = new HashSet<>();
        for (String p : personas) {
            if (p == null || p.isBlank()) {
                throw new IllegalArgumentException("Cada persona debe tener un nombre");
            }
            if (!vistas.add(p.trim().toLowerCase())) {
                throw new IllegalArgumentException("La persona '" + p.trim() + "' está repetida");
            }
        }
    }
}
