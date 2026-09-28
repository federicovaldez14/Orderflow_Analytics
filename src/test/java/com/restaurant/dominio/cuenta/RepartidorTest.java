package com.restaurant.dominio.cuenta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepartidorTest {

    @Test
    @DisplayName("Given $100.000 entre 3, When se reparte, Then 33.334 + 33.333 + 33.333 (el peso sobrante va al primero)")
    void shouldGiveRemainderToFirst() {
        // Act
        List<Long> partes = Repartidor.iguales(100_000, 3);
        // Assert
        assertEquals(List.of(33_334L, 33_333L, 33_333L), partes);
    }

    @Test
    @DisplayName("Given un total divisible, When se reparte en iguales, Then todas las partes son iguales")
    void shouldSplitEvenly() {
        assertEquals(List.of(25_000L, 25_000L, 25_000L, 25_000L), Repartidor.iguales(100_000, 4));
    }

    @Test
    @DisplayName("Given pesos 2:1, When se reparten $90.000, Then 60.000 y 30.000")
    void shouldSplitByWeight() {
        assertEquals(List.of(60_000L, 30_000L), Repartidor.repartir(90_000, List.of(2L, 1L)));
    }

    @Test
    @DisplayName("Given total 0 (límite), When se reparte, Then todos pagan 0")
    void shouldSplitZero() {
        assertEquals(List.of(0L, 0L), Repartidor.iguales(0, 2));
    }

    @Test
    @DisplayName("Given 1 persona (límite), When se reparte, Then paga todo")
    void shouldGiveAllToSinglePerson() {
        assertEquals(List.of(12_345L), Repartidor.iguales(12_345, 1));
    }

    @Test
    @DisplayName("Given $1 entre 3 (menos pesos que personas), When se reparte, Then 1, 0, 0")
    void shouldHandleTotalSmallerThanParts() {
        assertEquals(List.of(1L, 0L, 0L), Repartidor.iguales(1, 3));
    }

    @ParameterizedTest(name = "partes = {0}")
    @ValueSource(ints = {0, -1})
    @DisplayName("Given cero o menos partes, When se reparte, Then se rechaza")
    void shouldRejectNoParts(int partes) {
        assertThrows(IllegalArgumentException.class, () -> Repartidor.iguales(1000, partes));
    }

    @Test
    @DisplayName("Given un total negativo, un peso cero o sin pesos, When se reparte, Then se rechaza")
    void shouldRejectInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> Repartidor.iguales(-1, 2));
        assertThrows(IllegalArgumentException.class, () -> Repartidor.repartir(100, List.of(1L, 0L)));
        assertThrows(IllegalArgumentException.class, () -> Repartidor.repartir(100, List.of()));
    }
}
