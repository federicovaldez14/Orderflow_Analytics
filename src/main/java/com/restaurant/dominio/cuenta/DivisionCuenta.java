package com.restaurant.dominio.cuenta;

import java.util.List;

/**
 * Resultado de dividir una cuenta. El constructor verifica la invariante del
 * reto: la suma de lo que paga cada persona es EXACTAMENTE el total a cobrar.
 * Si una estrategia tuviera un error de redondeo, se detecta aquí y no en la
 * caja del restaurante.
 */
public record DivisionCuenta(int pedidoId, String metodo, long subtotal, int propinaPorcentaje, long propina,
                             List<ParteCuenta> partes) {

    public DivisionCuenta {
        partes = List.copyOf(partes);
        long consumo = partes.stream().mapToLong(ParteCuenta::consumo).sum();
        long propinas = partes.stream().mapToLong(ParteCuenta::propina).sum();
        if (consumo != subtotal || propinas != propina) {
            throw new IllegalStateException("La división no cuadra: partes " + (consumo + propinas)
                    + " vs total " + (subtotal + propina));
        }
    }

    public long total() {
        return subtotal + propina;
    }
}
