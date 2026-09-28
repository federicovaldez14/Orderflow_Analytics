package com.restaurant.dominio.cuenta;

import com.restaurant.dominio.modelo.Pedido;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cada persona paga el porcentaje que acordaron (p. ej. 60 / 40).
 * Regla: porcentajes enteros entre 1 y 100 que sumen exactamente 100.
 */
public class DivisionPorPorcentaje implements EstrategiaDivision {

    private final Map<String, Integer> porcentajes;

    public DivisionPorPorcentaje(Map<String, Integer> porcentajes) {
        if (porcentajes == null) {
            throw new IllegalArgumentException("Faltan los porcentajes");
        }
        Personas.validar(porcentajes.keySet());
        int suma = 0;
        for (Map.Entry<String, Integer> e : porcentajes.entrySet()) {
            Integer p = e.getValue();
            if (p == null || p < 1 || p > 100) {
                throw new IllegalArgumentException("El porcentaje de " + e.getKey() + " debe estar entre 1 y 100: " + p);
            }
            suma += p;
        }
        if (suma != 100) {
            throw new IllegalArgumentException("Los porcentajes deben sumar 100 y suman " + suma);
        }
        this.porcentajes = new LinkedHashMap<>(porcentajes);
    }

    @Override
    public String nombre() {
        return "POR_PORCENTAJE";
    }

    @Override
    public List<ParteCuenta> dividir(Pedido pedido) {
        List<String> nombres = new ArrayList<>(porcentajes.keySet());
        List<Long> pesos = new ArrayList<>();
        nombres.forEach(n -> pesos.add((long) porcentajes.get(n)));
        List<Long> montos = Repartidor.repartir(pedido.calcularTotal(), pesos);
        List<ParteCuenta> partes = new ArrayList<>();
        for (int i = 0; i < nombres.size(); i++) {
            partes.add(new ParteCuenta(nombres.get(i).trim(), montos.get(i), 0,
                    List.of(porcentajes.get(nombres.get(i)) + " % del total")));
        }
        return partes;
    }
}
