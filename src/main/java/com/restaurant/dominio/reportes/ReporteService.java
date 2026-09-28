package com.restaurant.dominio.reportes;

import com.restaurant.dominio.modelo.Pedido;

import java.util.List;

/** Contexto del Strategy: depende de la abstracción EstrategiaReporte (DIP). */
public class ReporteService {

    public Reporte generarReporte(EstrategiaReporte estrategia, List<Pedido> pedidos) {
        return estrategia.generar(pedidos);
    }
}
