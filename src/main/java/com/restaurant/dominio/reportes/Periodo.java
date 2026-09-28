package com.restaurant.dominio.reportes;

import com.restaurant.dominio.modelo.Pedido;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    /** Desde cuándo cuenta el periodo; vacío = todo el histórico. */
    public Optional<LocalDateTime> desde(LocalDateTime ahora) {
        switch (this) {
            case HOY:
                return Optional.of(ahora.toLocalDate().atStartOfDay());
            case SEMANA:
                return Optional.of(LocalDate.from(ahora).minusDays(6).atStartOfDay());
            default:
                return Optional.empty();
        }
    }

    public List<Pedido> filtrar(List<Pedido> pedidos, LocalDateTime ahora) {
        Optional<LocalDateTime> desde = desde(ahora);
        if (desde.isEmpty()) {
            return List.copyOf(pedidos);
        }
        return pedidos.stream().filter(p -> !p.getHoraCreacion().isBefore(desde.get())).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
