package com.restaurant.infraestructura.config;

import com.restaurant.aplicacion.puerto.salida.InventarioRepositorio;
import com.restaurant.aplicacion.puerto.salida.MenuRepositorio;
import com.restaurant.dominio.inventario.Ingrediente;
import com.restaurant.dominio.inventario.Receta;
import com.restaurant.dominio.inventario.UnidadMedida;
import com.restaurant.dominio.fabrica.PlatoFactory;
import com.restaurant.dominio.fabrica.TipoPlato;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carga la carta del restaurante (la misma de Corte 1), los ingredientes
 * y las recetas al arrancar, solo si la base está vacía.
 *
 * Unidades base: gramos (g), mililitros (ml) y unidades (und).
 */
@Component
@Order(1)
public class DatosIniciales implements ApplicationRunner {

    private final MenuRepositorio menu;
    private final InventarioRepositorio inventario;

    public DatosIniciales(MenuRepositorio menu, InventarioRepositorio inventario) {
        this.menu = menu;
        this.inventario = inventario;
    }

    @Override
    public void run(ApplicationArguments args) {
        cargarCarta();
        cargarInventario();
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

    public void cargarInventario() {
        if (!inventario.listarIngredientes().isEmpty()) {
            return;
        }
        ing("ARROZ", "Arroz", UnidadMedida.GRAMO, 20000, 3000);
        ing("FRIJOL", "Fríjol", UnidadMedida.GRAMO, 10000, 2000);
        ing("CARNE_MOLIDA", "Carne molida", UnidadMedida.GRAMO, 8000, 1500);
        ing("CHICHARRON", "Chicharrón", UnidadMedida.GRAMO, 6000, 1000);
        ing("HUEVO", "Huevo", UnidadMedida.UNIDAD, 60, 12);
        ing("PLATANO", "Plátano", UnidadMedida.UNIDAD, 40, 10);
        ing("AGUACATE", "Aguacate", UnidadMedida.UNIDAD, 20, 5);
        ing("POLLO", "Pollo", UnidadMedida.GRAMO, 12000, 2000);
        ing("PAPA", "Papa", UnidadMedida.GRAMO, 20000, 4000);
        ing("MAZORCA", "Mazorca", UnidadMedida.UNIDAD, 30, 6);
        ing("GALLINA", "Gallina", UnidadMedida.GRAMO, 10000, 2000);
        ing("YUCA", "Yuca", UnidadMedida.GRAMO, 8000, 1500);
        ing("HARINA_MAIZ", "Harina de maíz", UnidadMedida.GRAMO, 5000, 1000);
        ing("LIMON", "Limón", UnidadMedida.UNIDAD, 80, 20);
        ing("LECHE_COCO", "Leche de coco", UnidadMedida.MILILITRO, 5000, 1000);
        ing("GASEOSA", "Gaseosa (botella)", UnidadMedida.UNIDAD, 48, 12);
        ing("MORA", "Mora", UnidadMedida.GRAMO, 4000, 800);
        ing("AZUCAR", "Azúcar", UnidadMedida.GRAMO, 5000, 1000);
        ing("LECHE", "Leche", UnidadMedida.MILILITRO, 8000, 1500);
        ing("CAFE", "Café", UnidadMedida.GRAMO, 1000, 200);

        receta("Patacones con hogao", "PLATANO", 2);
        receta("Empanadas (x3)", "HARINA_MAIZ", 150, "CARNE_MOLIDA", 90, "PAPA", 60);
        receta("Bandeja Paisa", "ARROZ", 150, "FRIJOL", 120, "CARNE_MOLIDA", 100, "CHICHARRON", 120,
                "HUEVO", 1, "PLATANO", 1, "AGUACATE", 1);
        receta("Ajiaco santafereño", "POLLO", 200, "PAPA", 300, "MAZORCA", 1, "AGUACATE", 1);
        receta("Sancocho de gallina", "GALLINA", 250, "PAPA", 150, "YUCA", 150, "MAZORCA", 1, "PLATANO", 1);
        receta("Limonada de coco", "LIMON", 3, "LECHE_COCO", 120, "AZUCAR", 20);
        receta("Gaseosa", "GASEOSA", 1);
        receta("Jugo de mora", "MORA", 120, "AZUCAR", 25);
        receta("Flan de café", "HUEVO", 2, "LECHE", 150, "AZUCAR", 30, "CAFE", 10);
        receta("Postre de natas", "LECHE", 250, "AZUCAR", 40);
    }

    private void ing(String codigo, String nombre, UnidadMedida unidad, long stock, long minimo) {
        inventario.guardarIngrediente(new Ingrediente(codigo, nombre, unidad, stock, minimo));
    }

    private void receta(String plato, Object... codigoCantidad) {
        Map<String, Long> porPorcion = new LinkedHashMap<>();
        for (int i = 0; i < codigoCantidad.length; i += 2) {
            porPorcion.put((String) codigoCantidad[i], ((Number) codigoCantidad[i + 1]).longValue());
        }
        inventario.guardarReceta(new Receta(plato, porPorcion));
    }
}
