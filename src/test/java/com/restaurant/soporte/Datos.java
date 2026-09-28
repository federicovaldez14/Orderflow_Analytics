package com.restaurant.soporte;

import com.restaurant.dominio.fabrica.PlatoFactory;
import com.restaurant.dominio.fabrica.TipoPlato;
import com.restaurant.dominio.modelo.Plato;

/** Platos de la carta reutilizados por varias pruebas (datos de prueba, no lógica). */
public final class Datos {

    private Datos() {
    }

    public static final Plato BANDEJA = PlatoFactory.crear(TipoPlato.FUERTE, "Bandeja Paisa", 32000);
    public static final Plato AJIACO = PlatoFactory.crear(TipoPlato.FUERTE, "Ajiaco santafereño", 28000);
    public static final Plato LIMONADA = PlatoFactory.crear(TipoPlato.BEBIDA, "Limonada de coco", 9000);
    public static final Plato GASEOSA = PlatoFactory.crear(TipoPlato.BEBIDA, "Gaseosa", 5000);
    public static final Plato PATACONES = PlatoFactory.crear(TipoPlato.ENTRADA, "Patacones con hogao", 14000);
    public static final Plato FLAN = PlatoFactory.crear(TipoPlato.POSTRE, "Flan de café", 8000);
}
