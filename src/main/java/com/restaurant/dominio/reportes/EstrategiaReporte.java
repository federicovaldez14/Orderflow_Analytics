package com.restaurant.dominio.reportes;

import com.restaurant.dominio.modelo.Pedido;

import java.util.List;

/**
 * PATRÓN STRATEGY (Corte 1). Cada reporte es una clase intercambiable;
 * agregar uno nuevo (Reto 3 agregó cuatro) no modifica ReporteService ni los
 * existentes (OCP).
 *
 * Cambio de Corte 2: devuelve un Reporte (datos) en vez de un String, para
 * poder graficarlo.
 */
public interface EstrategiaReporte {
    Reporte generar(List<Pedido> pedidos);
}
