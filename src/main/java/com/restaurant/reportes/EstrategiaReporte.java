package com.restaurant.reportes;

import com.restaurant.modelo.Pedido;

import java.util.List;

/**
 * PATRÓN DE COMPORTAMIENTO (bonus, además de State): Strategy.
 *
 * Permite añadir un nuevo tipo de reporte (por ejemplo "ventas por hora")
 * sin modificar ReporteService ni las estrategias existentes: solo se crea
 * una clase nueva que implemente esta interfaz (Open/Closed Principle).
 */
public interface EstrategiaReporte {
    String generar(List<Pedido> pedidos);
}
