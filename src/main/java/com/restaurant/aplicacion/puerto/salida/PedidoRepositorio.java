package com.restaurant.aplicacion.puerto.salida;

import com.restaurant.dominio.modelo.Pedido;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para guardar y leer pedidos.
 *
 * Lo define la capa de aplicación con el vocabulario del negocio; no expone
 * nada de JDBC ni SQL. Hoy lo implementa PedidoRepositorioJdbc (H2); cambiar a
 * PostgreSQL o a otra tecnología es escribir otro adaptador, sin tocar
 * GestorPedidos ni el dominio.
 */
public interface PedidoRepositorio {

    /** Reserva el siguiente identificador de pedido. */
    int siguienteId();

    /** Inserta o actualiza el pedido completo (cabecera + líneas). */
    void guardar(Pedido pedido);

    Optional<Pedido> buscarPorId(int id);

    /** Pedido no terminal (ni Entregado ni Cancelado) de una mesa, si existe. */
    Optional<Pedido> buscarActivoPorMesa(int mesa);

    List<Pedido> listarTodos();
}
