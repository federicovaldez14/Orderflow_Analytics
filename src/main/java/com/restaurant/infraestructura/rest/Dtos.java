package com.restaurant.infraestructura.rest;

import com.restaurant.dominio.inventario.Ingrediente;
import com.restaurant.dominio.inventario.StockInsuficienteException;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.modelo.Plato;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public record ErrorStockDto(int status, String error, String mensaje,
                                List<StockInsuficienteException.Faltante> faltantes) {
    }

    // ---------- Inventario (Reto 1) ----------

    public record IngredienteDto(String codigo, String nombre, String unidad, long stock, long stockMinimo,
                                 boolean bajoMinimo) {
        static IngredienteDto de(Ingrediente i) {
            return new IngredienteDto(i.getCodigo(), i.getNombre(), i.getUnidad().getSimbolo(), i.getStock(),
                    i.getStockMinimo(), i.bajoMinimo());
        }
    }

    public record CantidadDto(long cantidad, String nota) {
    }

    public record ConteoDto(long stockContado, String nota) {
    }

    // ---------- División de cuenta (Reto 2) ----------

    public record AsignacionDto(int linea, Map<String, Integer> personas) {
    }

    /**
     * metodo: IGUALITARIA (personas o numeroPersonas), POR_CONSUMO (asignaciones)
     * o POR_PORCENTAJE (porcentajes). propinaPorcentaje es opcional (0 por defecto).
     */
    public record SolicitudDivisionDto(String metodo, List<String> personas, Integer numeroPersonas,
                                       List<AsignacionDto> asignaciones, Map<String, Integer> porcentajes,
                                       Integer propinaPorcentaje) {
    }

    public record ParteDto(String persona, long consumo, long propina, long total, List<String> detalle) {
    }

    public record DivisionDto(int pedidoId, String metodo, long subtotal, int propinaPorcentaje, long propina,
                              long total, List<ParteDto> partes) {
        static DivisionDto de(com.restaurant.dominio.cuenta.DivisionCuenta d) {
            List<ParteDto> partes = new ArrayList<>();
            d.partes().forEach(p -> partes.add(new ParteDto(p.persona(), p.consumo(), p.propina(), p.total(),
                    p.detalle())));
            return new DivisionDto(d.pedidoId(), d.metodo(), d.subtotal(), d.propinaPorcentaje(), d.propina(),
                    d.total(), partes);
        }
    }
}
