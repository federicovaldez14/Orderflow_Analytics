package com.restaurant.dominio.cuenta;

import com.restaurant.dominio.modelo.Pedido;

import java.util.List;

/**
 * PATRÓN STRATEGY (igual que EstrategiaReporte del Corte 1): cada forma de
 * dividir la cuenta es una clase intercambiable. Agregar una nueva (p. ej.
 * "cada quien paga sus bebidas y el resto por igual") es crear una clase,
 * sin tocar DivisorCuenta ni las estrategias existentes (OCP).
 *
 * Cada estrategia reparte SOLO el consumo (subtotal del pedido); la
 * propina la reparte DivisorCuenta en proporción a lo consumido.
 */
public interface EstrategiaDivision {

    /** Nombre para mostrar (IGUALITARIA, POR_CONSUMO, POR_PORCENTAJE). */
    String nombre();

    /** Partes del consumo; la suma DEBE ser exactamente pedido.calcularTotal(). */
    List<ParteCuenta> dividir(Pedido pedido);
}
