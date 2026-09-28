package com.restaurant.dominio.inventario;

import com.restaurant.dominio.modelo.ItemPedido;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Regla de negocio: cuánto inventario consume un conjunto de líneas de
 * pedido. Suma por ingrediente (dos platos que usan arroz descuentan el
 * arroz una sola vez, con la cantidad total).
 *
 * Se devuelve un TreeMap (ordenado por código): el adaptador de base de
 * datos bloquea las filas SIEMPRE en el mismo orden, lo que evita
 * interbloqueos cuando dos pedidos concurrentes piden los mismos
 * ingredientes en distinto orden.
 *
 * Los platos sin receta registrada no descuentan inventario (p. ej. un plato
 * nuevo que aún no se ha costeado); quedan listados en platosSinReceta().
 */
public final class CalculadoraConsumo {

    private CalculadoraConsumo() {
    }

    public static Map<String, Long> calcular(List<ItemPedido> items, Map<String, Receta> recetasPorPlato) {
        Map<String, Long> total = new TreeMap<>();
        for (ItemPedido item : items) {
            Receta receta = recetasPorPlato.get(item.getPlato().getNombre());
            if (receta == null) {
                continue;
            }
            receta.consumoPara(item.getCantidad()).forEach((codigo, cantidad) -> total.merge(codigo, cantidad, Long::sum));
        }
        return total;
    }

    /** Porciones que alcanzan a prepararse de un plato con el stock actual. */
    public static long porcionesDisponibles(Receta receta, Map<String, Long> stockPorCodigo) {
        long minimo = Long.MAX_VALUE;
        for (Map.Entry<String, Long> e : receta.getPorPorcion().entrySet()) {
            long stock = stockPorCodigo.getOrDefault(e.getKey(), 0L);
            minimo = Math.min(minimo, stock / e.getValue());
        }
        return minimo == Long.MAX_VALUE ? 0 : minimo;
    }
}
