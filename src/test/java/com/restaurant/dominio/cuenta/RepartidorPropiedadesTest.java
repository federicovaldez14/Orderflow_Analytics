package com.restaurant.dominio.cuenta;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas basadas en propiedades (jqwik, como en el taller de pruebas
 * unitarias): en vez de unos pocos ejemplos, se generan cientos de totales y
 * números de personas y se comprueba lo que SIEMPRE debe cumplirse.
 */
class RepartidorPropiedadesTest {

    @Property(tries = 500)
    void lasPartesIgualesSumanElTotalYDifierenMaximoUnPeso(
            @ForAll @LongRange(min = 0, max = 50_000_000) long total,
            @ForAll @IntRange(min = 1, max = 20) int personas) {
        List<Long> partes = Repartidor.iguales(total, personas);

        assertEquals(total, partes.stream().mapToLong(Long::longValue).sum());
        long max = partes.stream().mapToLong(Long::longValue).max().getAsLong();
        long min = partes.stream().mapToLong(Long::longValue).min().getAsLong();
        assertTrue(max - min <= 1, "diferencia " + (max - min));
        assertTrue(min >= 0);
    }

    @Property(tries = 500)
    void elRepartoPonderadoSiempreSumaElTotal(
            @ForAll @LongRange(min = 0, max = 50_000_000) long total,
            @ForAll @IntRange(min = 1, max = 20) int personas,
            @ForAll @IntRange(min = 1, max = 100) int pesoBase) {
        List<Long> pesos = new ArrayList<>();
        for (int i = 0; i < personas; i++) {
            pesos.add((long) (pesoBase + i * 7 % 13));
        }

        List<Long> partes = Repartidor.repartir(total, pesos);

        assertEquals(total, partes.stream().mapToLong(Long::longValue).sum());
        assertEquals(personas, partes.size());
    }
}
