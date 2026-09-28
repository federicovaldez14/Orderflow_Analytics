package com.restaurant.soporte;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Arma cuerpos JSON como Map para las pruebas de caja negra (sin usar clases internas). */
public final class Json {

    private Json() {
    }

    public static Map<String, Object> linea(String plato, int cantidad) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("plato", plato);
        m.put("cantidad", cantidad);
        return m;
    }

    @SafeVarargs
    public static Map<String, Object> pedido(int mesa, Map<String, Object>... lineas) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mesa", mesa);
        m.put("lineas", Arrays.asList(lineas));
        return m;
    }

    public static Map<String, Object> mapa(Object... claveValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            m.put((String) claveValor[i], claveValor[i + 1]);
        }
        return m;
    }

    public static List<Object> lista(Object... elementos) {
        return Arrays.asList(elementos);
    }
}
