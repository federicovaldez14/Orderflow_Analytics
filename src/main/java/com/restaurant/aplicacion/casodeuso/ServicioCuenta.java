package com.restaurant.aplicacion.casodeuso;

import com.restaurant.aplicacion.excepcion.RecursoNoEncontradoException;
import com.restaurant.aplicacion.excepcion.ReglaNegocioException;
import com.restaurant.aplicacion.puerto.salida.PedidoRepositorio;
import com.restaurant.dominio.cuenta.DivisionCuenta;
import com.restaurant.dominio.cuenta.DivisorCuenta;
import com.restaurant.dominio.cuenta.EstrategiaDivision;
import com.restaurant.dominio.modelo.Pedido;

/**
 * Caso de uso del RETO 2: dividir la cuenta de un pedido "al gusto" de la
 * mesa. El cálculo es del dominio (DivisorCuenta + estrategias); aquí solo
 * se busca el pedido por el puerto y se traducen los rechazos del negocio.
 *
 * Es una CONSULTA (no guarda nada): se puede recalcular cuantas veces la
 * mesa cambie de opinión.
 */
public class ServicioCuenta {

    private final PedidoRepositorio pedidos;

    public ServicioCuenta(PedidoRepositorio pedidos) {
        this.pedidos = pedidos;
    }

    public DivisionCuenta dividir(int pedidoId, EstrategiaDivision estrategia, int propinaPorcentaje) {
        Pedido pedido = pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido #" + pedidoId));
        try {
            return DivisorCuenta.dividir(pedido, estrategia, propinaPorcentaje);
        } catch (IllegalStateException e) {
            throw new ReglaNegocioException(e.getMessage());
        }
    }
}
