package com.restaurant.dominio.estado;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadosPedidoTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"Creado", "En preparación", "Listo", "Entregado", "Cancelado"})
    @DisplayName("Given el nombre guardado de un estado, When se traduce, Then se obtiene el mismo estado")
    void shouldRoundTripNames(String nombre) {
        assertEquals(nombre, EstadosPedido.desdeNombre(nombre).getNombre());
    }

    @Test
    @DisplayName("Given un nombre desconocido o null, When se traduce, Then se rechaza")
    void shouldRejectUnknownName() {
        assertThrows(IllegalArgumentException.class, () -> EstadosPedido.desdeNombre("Perdido"));
        assertThrows(IllegalArgumentException.class, () -> EstadosPedido.desdeNombre(null));
    }

    @Test
    @DisplayName("Solo Entregado y Cancelado son terminales")
    void shouldKnowTerminalStates() {
        assertTrue(new EstadoEntregado().esTerminal());
        assertTrue(new EstadoCancelado().esTerminal());
        assertFalse(new EstadoCreado().esTerminal());
        assertFalse(new EstadoEnPreparacion().esTerminal());
        assertFalse(new EstadoListo().esTerminal());
    }
}
