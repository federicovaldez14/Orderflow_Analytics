package com.restaurant.dominio.reportes;

import java.util.List;

/**
 * Resultado de una estrategia de reporte, como DATOS (no como texto).
 *
 * Corte 1 devolvía un String listo para imprimir; para poder graficar (Reto 3)
 * el reporte ahora es una lista de (etiqueta, valor). Cada adaptador decide
 * cómo mostrarlo: la UI dibuja barras, la API lo entrega como JSON y aTexto()
 * conserva la salida de consola de Corte 1.
 *
 * @param id     identificador estable (para la API y las pruebas)
 * @param unidad cómo leer los valores: "unidades", "$", "min", "pedidos"
 */
public record Reporte(String id, String titulo, String unidad, List<Dato> datos) {

    public record Dato(String etiqueta, double valor) {
    }

    public Reporte {
        datos = List.copyOf(datos);
    }

    public boolean vacio() {
        return datos.isEmpty();
    }

    public String aTexto() {
        StringBuilder sb = new StringBuilder("== ").append(titulo).append(" ==\n");
        if (datos.isEmpty()) {
            return sb.append("Sin datos.\n").toString();
        }
        for (Dato d : datos) {
            sb.append(d.etiqueta()).append(": ").append(formatear(d.valor())).append(' ').append(unidad).append('\n');
        }
        return sb.toString();
    }

    private static String formatear(double v) {
        return v == Math.rint(v) ? String.format("%,d", (long) v) : String.format("%,.1f", v);
    }
}
