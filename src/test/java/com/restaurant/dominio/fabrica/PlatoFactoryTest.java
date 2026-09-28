package com.restaurant.dominio.fabrica;

import com.restaurant.dominio.modelo.Plato;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Factory Method: cada categoría recibe su tiempo de preparación. */
class PlatoFactoryTest {

    @ParameterizedTest(name = "{0} -> {1} min")
    @CsvSource({"ENTRADA, 8", "FUERTE, 18", "BEBIDA, 2", "POSTRE, 6"})
    @DisplayName("Given una categoría, When la fábrica crea el plato, Then asigna su tiempo de preparación")
    void shouldAssignPreparationTimeByType(TipoPlato tipo, int minutos) {
        // Act
        Plato plato = PlatoFactory.crear(tipo, "Plato de prueba", 10_000);
        // Assert
        assertEquals(minutos, plato.getTiempoPreparacionMinutos());
        assertEquals(tipo, plato.getTipo());
    }

    @ParameterizedTest(name = "precio = {0}")
    @ValueSource(longs = {0, -1})
    @DisplayName("Given un precio cero o negativo, When se crea el plato, Then se rechaza")
    void shouldRejectNonPositivePrice(long precio) {
        assertThrows(IllegalArgumentException.class, () -> PlatoFactory.crear(TipoPlato.FUERTE, "X", precio));
    }

    @Test
    @DisplayName("Given precio 1 (límite inferior válido), When se crea el plato, Then se acepta")
    void shouldAcceptMinimumPrice() {
        assertEquals(1L, PlatoFactory.crear(TipoPlato.BEBIDA, "Agua", 1).getPrecio());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Given un nombre vacío o null, When se crea el plato, Then se rechaza")
    void shouldRejectBlankName(String nombre) {
        assertThrows(IllegalArgumentException.class, () -> PlatoFactory.crear(TipoPlato.FUERTE, nombre, 1000));
    }

    @Test
    @DisplayName("Given un tipo null, When se crea el plato, Then se rechaza")
    void shouldRejectNullType() {
        assertThrows(IllegalArgumentException.class, () -> PlatoFactory.crear(null, "X", 1000));
    }

    @Test
    @DisplayName("Given dos platos con el mismo nombre, When se comparan, Then son el mismo plato de la carta")
    void shouldCompareByName() {
        assertEquals(PlatoFactory.crear(TipoPlato.FUERTE, "Ajiaco", 1000),
                PlatoFactory.crear(TipoPlato.FUERTE, " Ajiaco ", 2000));
    }
}
