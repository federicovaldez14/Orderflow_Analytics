package com.restaurant.dominio.inventario;

/**
 * Insumo del restaurante con su existencia actual y el mínimo a partir del
 * cual hay que reponer.
 *
 * Invariante: el stock nunca es negativo. Descontar más de lo que hay lanza
 * StockInsuficienteException (y la base de datos tiene además un CHECK
 * stock >= 0 como segunda barrera).
 */
public class Ingrediente {

    private final String codigo;
    private final String nombre;
    private final UnidadMedida unidad;
    private long stock;
    private final long stockMinimo;

    public Ingrediente(String codigo, String nombre, UnidadMedida unidad, long stock, long stockMinimo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código del ingrediente es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del ingrediente es obligatorio");
        }
        if (unidad == null) {
            throw new IllegalArgumentException("La unidad de medida es obligatoria");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo: " + stock);
        }
        if (stockMinimo < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo: " + stockMinimo);
        }
        this.codigo = codigo.trim().toUpperCase();
        this.nombre = nombre.trim();
        this.unidad = unidad;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
    }

    public void descontar(long cantidad) {
        exigirPositiva(cantidad);
        if (cantidad > stock) {
            throw new StockInsuficienteException(java.util.List.of(
                    new StockInsuficienteException.Faltante(codigo, nombre, cantidad, stock, unidad)));
        }
        stock -= cantidad;
    }

    public void reponer(long cantidad) {
        exigirPositiva(cantidad);
        stock += cantidad;
    }

    /** true cuando el stock está por DEBAJO del mínimo (igual al mínimo todavía no alerta). */
    public boolean bajoMinimo() {
        return stock < stockMinimo;
    }

    /**
     * true si esta salida hace que el ingrediente cruce el mínimo hacia abajo.
     * Sirve para avisar UNA sola vez y no en cada venta posterior.
     */
    public static boolean cruzoMinimo(long stockAntes, long stockDespues, long minimo) {
        return stockAntes >= minimo && stockDespues < minimo;
    }

    private static void exigirPositiva(long cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero: " + cantidad);
        }
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public UnidadMedida getUnidad() {
        return unidad;
    }

    public long getStock() {
        return stock;
    }

    public long getStockMinimo() {
        return stockMinimo;
    }

    @Override
    public String toString() {
        return nombre + " (" + stock + " " + unidad.getSimbolo() + ")";
    }
}
