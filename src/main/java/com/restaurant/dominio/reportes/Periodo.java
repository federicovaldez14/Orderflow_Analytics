package com.restaurant.dominio.reportes;

import com.restaurant.dominio.modelo.Pedido;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Filtro de tiempo del panel de analítica, según la hora de creación del pedido. */
public enum Periodo {
    HOY("Hoy"),
    SEMANA("Últimos 7 días"),
    TODO("Todo el histórico");

    private final String etiqueta;

    Periodo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public List<Pedido> filtrar(List<Pedido> pedidos, LocalDateTime ahora) {
        LocalDateTime desde;
        switch (this) {
            case HOY:
                desde = ahora.toLocalDate().atStartOfDay();
                break;
            case SEMANA:
                desde = LocalDate.from(ahora).minusDays(6).atStartOfDay();
                break;
            default:
                return List.copyOf(pedidos);
        }
        return pedidos.stream().filter(p -> !p.getHoraCreacion().isBefore(desde)).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
