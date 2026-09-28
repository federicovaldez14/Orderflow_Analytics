package com.restaurant.dominio.inventario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredienteTest {

    private Ingrediente huevos;

    @BeforeEach
    void setUp() {
        huevos = new Ingrediente("huevo", "Huevo", UnidadMedida.UNIDAD, 10, 4);
    }

    @Test
    @DisplayName("Given 10 huevos, When se descuentan 3, Then quedan 7")
    void shouldDiscountStock() {
        huevos.descontar(3);
        assertEquals(7, huevos.getStock());
    }

    @Test
    @DisplayName("Given 10 huevos, When se descuentan exactamente 10 (límite), Then queda en 0")
    void shouldAllowDiscountingAll() {
        huevos.descontar(10);
        assertEquals(0, huevos.getStock());
    }

    @Test
    @DisplayName("Given 10 huevos, When se piden 11 (límite + 1), Then se rechaza y el stock no cambia")
    void shouldRejectDiscountAboveStock() {
        StockInsuficienteException e = assertThrows(StockInsuficienteException.class, () -> huevos.descontar(11));
        assertEquals(10, huevos.getStock());
        assertEquals(11, e.getFaltantes().get(0).requerido());
        assertEquals(10, e.getFaltantes().get(0).disponible());
    }

    @ParameterizedTest(name = "cantidad = {0}")
    @ValueSource(longs = {0, -5})
    @DisplayName("Given una cantidad cero o negativa, When se descuenta o repone, Then se rechaza")
    void shouldRejectNonPositiveQuantities(long cantidad) {
        assertThrows(IllegalArgumentException.class, () -> huevos.descontar(cantidad));
        assertThrows(IllegalArgumentException.class, () -> huevos.reponer(cantidad));
    }

    @Test
    @DisplayName("Given 10 huevos, When se reponen 12, Then hay 22")
    void shouldRestock() {
        huevos.reponer(12);
        assertEquals(22, huevos.getStock());
    }

    @ParameterizedTest(name = "stock {0}, mínimo 4 -> bajo = {1}")
    @CsvSource({"3, true", "4, false", "5, false", "0, true"})
    @DisplayName("El ingrediente está bajo mínimo solo cuando el stock es MENOR al mínimo")
    void shouldDetectLowStock(long stock, boolean esperado) {
        Ingrediente ing = new Ingrediente("X", "X", UnidadMedida.GRAMO, stock, 4);
        assertEquals(esperado, ing.bajoMinimo());
    }

    @ParameterizedTest(name = "antes {0}, después {1}, mínimo 4 -> cruzó = {2}")
    @CsvSource({"5, 3, true", "4, 3, true", "5, 4, false", "3, 2, false", "10, 9, false"})
    @DisplayName("Solo se alerta la venta que cruza el mínimo, no las siguientes")
    void shouldDetectThresholdCrossing(long antes, long despues, boolean esperado) {
        assertEquals(esperado, Ingrediente.cruzoMinimo(antes, despues, 4));
    }

    @Test
    @DisplayName("Given datos inválidos, When se crea el ingrediente, Then se rechaza")
    void shouldValidateConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new Ingrediente(" ", "A", UnidadMedida.GRAMO, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Ingrediente("A", "", UnidadMedida.GRAMO, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Ingrediente("A", "A", null, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Ingrediente("A", "A", UnidadMedida.GRAMO, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Ingrediente("A", "A", UnidadMedida.GRAMO, 1, -1));
    }

    @Test
    @DisplayName("El código se normaliza a mayúsculas")
    void shouldNormalizeCode() {
        assertEquals("HUEVO", huevos.getCodigo());
        assertFalse(huevos.bajoMinimo());
        assertTrue(new Ingrediente("a", "A", UnidadMedida.GRAMO, 0, 1).bajoMinimo());
    }
}
