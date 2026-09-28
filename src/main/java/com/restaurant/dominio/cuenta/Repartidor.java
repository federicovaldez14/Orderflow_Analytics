package com.restaurant.dominio.cuenta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Reparte un valor entero (pesos) en partes proporcionales a unos pesos,
 * garantizando que las partes SUMEN EXACTAMENTE el total.
 *
 * Método del resto mayor: cada parte recibe la porción entera que le toca y
 * los pesos que sobran por redondeo se entregan de a uno a las partes con
 * mayor residuo (a igual residuo, a la primera). Así $100.000 entre 3 da
 * 33.334 + 33.333 + 33.333 y nunca 99.999 ni 100.001.
 */
public final class Repartidor {

    private Repartidor() {
    }

    public static List<Long> repartir(long total, List<Long> pesos) {
        if (total < 0) {
            throw new IllegalArgumentException("El total a repartir no puede ser negativo: " + total);
        }
        if (pesos == null || pesos.isEmpty()) {
            throw new IllegalArgumentException("Debe haber al menos una parte");
        }
        long sumaPesos = 0;
        for (Long p : pesos) {
            if (p == null || p <= 0) {
                throw new IllegalArgumentException("Cada peso debe ser mayor que cero: " + p);
            }
            sumaPesos += p;
        }
        List<Long> partes = new ArrayList<>();
        List<Long> residuos = new ArrayList<>();
        long asignado = 0;
        for (Long p : pesos) {
            // total * p puede desbordar long con valores enormes; Math.multiplyHigh no hace falta
            // para cuentas de restaurante, pero se usa aritmética exacta para fallar en vez de mentir.
            long producto = Math.multiplyExact(total, p);
            long parte = producto / sumaPesos;
            partes.add(parte);
            residuos.add(producto % sumaPesos);
            asignado += parte;
        }
        long sobrante = total - asignado;   // siempre < número de partes
        final long suma = sumaPesos;
        List<Integer> orden = IntStream.range(0, pesos.size()).boxed()
                .sorted(Comparator.comparingLong((Integer i) -> residuos.get(i)).reversed()
                        .thenComparingInt(i -> i))
                .collect(Collectors.toList());
        for (int k = 0; k < sobrante; k++) {
            int i = orden.get(k);
            partes.set(i, partes.get(i) + 1);
        }
        return partes;
    }

    /** Reparto en partes iguales (todos con peso 1). */
    public static List<Long> iguales(long total, int partes) {
        if (partes < 1) {
            throw new IllegalArgumentException("Debe haber al menos una parte: " + partes);
        }
        List<Long> pesos = new ArrayList<>();
        for (int i = 0; i < partes; i++) {
            pesos.add(1L);
        }
        return repartir(total, pesos);
    }
}
