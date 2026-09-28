package com.restaurant.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.restaurant.soporte.Datos.BANDEJA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Clases de equivalencia de la cantidad: [1..50] válida; <1 y >50 inválidas. */
class ItemPedidoTest {

    @ParameterizedTest(name = "cantidad = {0}")
    @ValueSource(ints = {1, 2, 49, 50})
    @DisplayName("Given una cantidad dentro de [1, 50] (incluye límites), When se crea el ítem, Then se acepta")
    void shouldAcceptValidQuantities(int cantidad) {
        ItemPedido item = new ItemPedido(BANDEJA, cantidad);
        assertEquals(cantidad, item.getCantidad());
    }

    @ParameterizedTest(name = "cantidad = {0}")
    @ValueSource(ints = {0, -1, 51, 1000})
    @DisplayName("Given una cantidad fuera de [1, 50], When se crea el ítem, Then se rechaza")
    void shouldRejectInvalidQuantities(int cantidad) {
        assertThrows(IllegalArgumentException.class, () -> new ItemPedido(BANDEJA, cantidad));
    }

    @Test
    @DisplayName("Given un plato null, When se crea el ítem, Then se rechaza")
    void shouldRejectNullDish() {
        assertThrows(IllegalArgumentException.class, () -> new ItemPedido(null, 1));
    }

    @Test
    @DisplayName("Given 3 bandejas de $32.000, When se calcula el subtotal, Then es $96.000")
    void shouldComputeSubtotal() {
        // Arrange
        ItemPedido item = new ItemPedido(BANDEJA, 3);
        // Act
        long subtotal = item.subtotal();
        // Assert
        assertEquals(96_000L, subtotal);
    }
}
