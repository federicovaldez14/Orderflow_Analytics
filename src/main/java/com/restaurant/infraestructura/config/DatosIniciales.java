package com.restaurant.infraestructura.config;

import com.restaurant.aplicacion.puerto.salida.MenuRepositorio;
import com.restaurant.dominio.fabrica.PlatoFactory;
import com.restaurant.dominio.fabrica.TipoPlato;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Carga la carta del restaurante al arrancar si la base está vacía
 * (la misma carta que tenía MenuRestaurante en Corte 1).
 */
@Component
@Order(1)
public class DatosIniciales implements ApplicationRunner {

    private final MenuRepositorio menu;

    public DatosIniciales(MenuRepositorio menu) {
        this.menu = menu;
    }

    @Override
    public void run(ApplicationArguments args) {
        cargarCarta();
    }

    public void cargarCarta() {
        if (!menu.listar().isEmpty()) {
            return;
        }
        menu.guardar(PlatoFactory.crear(TipoPlato.ENTRADA, "Patacones con hogao", 14000));
        menu.guardar(PlatoFactory.crear(TipoPlato.ENTRADA, "Empanadas (x3)", 9000));
        menu.guardar(PlatoFactory.crear(TipoPlato.FUERTE, "Bandeja Paisa", 32000));
        menu.guardar(PlatoFactory.crear(TipoPlato.FUERTE, "Ajiaco santafereño", 28000));
        menu.guardar(PlatoFactory.crear(TipoPlato.FUERTE, "Sancocho de gallina", 27000));
        menu.guardar(PlatoFactory.crear(TipoPlato.BEBIDA, "Limonada de coco", 9000));
        menu.guardar(PlatoFactory.crear(TipoPlato.BEBIDA, "Gaseosa", 5000));
        menu.guardar(PlatoFactory.crear(TipoPlato.BEBIDA, "Jugo de mora", 7000));
        menu.guardar(PlatoFactory.crear(TipoPlato.POSTRE, "Flan de café", 8000));
        menu.guardar(PlatoFactory.crear(TipoPlato.POSTRE, "Postre de natas", 7000));
    }
}
