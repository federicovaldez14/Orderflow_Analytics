package com.restaurant.aplicacion.puerto.salida;

import com.restaurant.dominio.inventario.Ingrediente;
import com.restaurant.dominio.inventario.MovimientoInventario;
import com.restaurant.dominio.inventario.Receta;
import com.restaurant.dominio.inventario.StockInsuficienteException;
import com.restaurant.dominio.inventario.TipoMovimiento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Puerto de salida del inventario.
 *
 * Las operaciones que cambian stock reciben TODO el consumo de una vez y
 * deben ser atómicas: o se descuentan todos los ingredientes del pedido o
 * ninguno. Esa garantía (y la de no vender dos veces el último huevo cuando
 * llegan dos pedidos a la vez) es responsabilidad del adaptador, porque solo
 * la base de datos puede darla entre varios hilos o procesos.
 */
public interface InventarioRepositorio {

    /** Motivo común de un grupo de movimientos. */
    record Motivo(TipoMovimiento tipo, Integer pedidoId, String descripcion, LocalDateTime fecha) {
    }

    List<Ingrediente> listarIngredientes();

    Optional<Ingrediente> buscarIngrediente(String codigo);

    void guardarIngrediente(Ingrediente ingrediente);

    Map<String, Receta> recetasPorPlato();

    void guardarReceta(Receta receta);

    /**
     * Descuenta el consumo completo o nada.
     * @return stock resultante por código de ingrediente
     * @throws StockInsuficienteException si al menos un ingrediente no alcanza
     */
    Map<String, Long> descontar(Map<String, Long> consumo, Motivo motivo);

    /** Suma al stock (entradas, devoluciones, ajustes positivos). */
    Map<String, Long> sumar(Map<String, Long> cantidades, Motivo motivo);

    /** Registra movimientos sin cambiar el stock (merma de algo ya descontado). */
    void registrarSinCambio(Map<String, Long> cantidades, Motivo motivo);

    /** Últimos movimientos, del más reciente al más antiguo. codigo null = todos. */
    List<MovimientoInventario> movimientos(String codigo, int limite);

    List<MovimientoInventario> movimientosDePedido(int pedidoId);
}
