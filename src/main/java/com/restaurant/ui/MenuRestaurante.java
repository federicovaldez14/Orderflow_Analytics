package com.restaurant.ui;

import com.restaurant.fabrica.PlatoFactory;
import com.restaurant.fabrica.TipoPlato;
import com.restaurant.modelo.Plato;

import java.util.Arrays;
import java.util.List;

/**
 * Catálogo fijo de platos y mesas, como si ya estuvieran dados de alta en
 * el sistema (igual que en un POS real: el mesero no inventa el menú
 * cada vez que atiende una mesa, solo lo consulta).
 */
public final class MenuRestaurante {

    private MenuRestaurante() {
    }

    public static final List<Plato> PLATOS = Arrays.asList(
            PlatoFactory.crear(TipoPlato.ENTRADA, "Patacones con hogao", 14000),
            PlatoFactory.crear(TipoPlato.ENTRADA, "Empanadas (x3)", 9000),
            PlatoFactory.crear(TipoPlato.FUERTE, "Bandeja Paisa", 32000),
            PlatoFactory.crear(TipoPlato.FUERTE, "Ajiaco santafereño", 28000),
            PlatoFactory.crear(TipoPlato.FUERTE, "Sancocho de gallina", 27000),
            PlatoFactory.crear(TipoPlato.BEBIDA, "Limonada de coco", 9000),
            PlatoFactory.crear(TipoPlato.BEBIDA, "Gaseosa", 5000),
            PlatoFactory.crear(TipoPlato.BEBIDA, "Jugo de mora", 7000),
            PlatoFactory.crear(TipoPlato.POSTRE, "Flan de café", 8000),
            PlatoFactory.crear(TipoPlato.POSTRE, "Postre de natas", 7000)
    );

    public static final List<Integer> MESAS = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
}
