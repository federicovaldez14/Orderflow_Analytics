package com.restaurant.infraestructura.rest;

import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.modelo.Plato;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Objetos de transferencia del adaptador REST. Se mantienen separados del
 * dominio para que un cambio en el JSON no obligue a cambiar las entidades
 * (y viceversa).
 */
public final class Dtos {

    private Dtos() {
    }

    public record PlatoDto(String nombre, String tipo, long precio, int tiempoPreparacionMinutos) {
        static PlatoDto de(Plato p) {
            return new PlatoDto(p.getNombre(), p.getTipo().name(), p.getPrecio(), p.getTiempoPreparacionMinutos());
        }
    }

    public record LineaDto(String plato, int cantidad) {
    }

    public record CrearPedidoDto(int mesa, List<LineaDto> lineas) {
    }

    public record LineaPedidoDto(int linea, String plato, int cantidad, long precioUnitario, long subtotal) {
    }

    public record PedidoDto(int id, int mesa, String estado, long total, List<LineaPedidoDto> lineas,
                            LocalDateTime horaCreacion, LocalDateTime horaEntregado) {
        static PedidoDto de(Pedido p) {
            List<LineaPedidoDto> lineas = new ArrayList<>();
            int i = 0;
            for (ItemPedido item : p.getItems()) {
                lineas.add(new LineaPedidoDto(i++, item.getPlato().getNombre(), item.getCantidad(),
                        item.getPlato().getPrecio(), item.subtotal()));
            }
            return new PedidoDto(p.getId(), p.getMesa(), p.getEstadoNombre(), p.calcularTotal(), lineas,
                    p.getHoraCreacion(), p.getHoraEntregado());
        }
    }

    public record MesaDto(int mesa, boolean libre, Integer pedidoId, String estado, long total) {
    }

    public record ErrorDto(int status, String error, String mensaje) {
    }
}
