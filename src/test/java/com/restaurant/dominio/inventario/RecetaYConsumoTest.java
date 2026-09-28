package com.restaurant.dominio.inventario;

import com.restaurant.dominio.modelo.ItemPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static com.restaurant.soporte.Datos.BANDEJA;
import static com.restaurant.soporte.Datos.FLAN;
import static com.restaurant.soporte.Datos.GASEOSA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecetaYConsumoTest {

    private static final Receta RECETA_BANDEJA = new Receta("Bandeja Paisa",
            Map.of("ARROZ", 150L, "HUEVO", 1L));
    private static final Receta RECETA_FLAN = new Receta("Flan de café",
            Map.of("HUEVO", 2L, "LECHE", 150L));
    private static final Map<String, Receta> RECETAS = Map.of(
            "Bandeja Paisa", RECETA_BANDEJA, "Flan de café", RECETA_FLAN);

    @Test
    @DisplayName("Given 3 porciones, When se calcula el consumo, Then multiplica cada ingrediente por 3")
    void shouldMultiplyByPortions() {
        Map<String, Long> consumo = RECETA_BANDEJA.consumoPara(3);
        assertEquals(450L, consumo.get("ARROZ"));
        assertEquals(3L, consumo.get("HUEVO"));
    }

    @ParameterizedTest(name = "porciones = {0}")
    @ValueSource(ints = {0, -1})
    @DisplayName("Given cero o menos porciones, When se calcula el consumo, Then se rechaza")
    void shouldRejectNonPositivePortions(int porciones) {
        assertThrows(IllegalArgumentException.class, () -> RECETA_BANDEJA.consumoPara(porciones));
    }

    @Test
    @DisplayName("Given una receta vacía o con cantidades no positivas, When se crea, Then se rechaza")
    void shouldValidateRecipe() {
        assertThrows(IllegalArgumentException.class, () -> new Receta("X", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new Receta("X", Map.of("ARROZ", 0L)));
        assertThrows(IllegalArgumentException.class, () -> new Receta(" ", Map.of("ARROZ", 1L)));
    }

    @Test
    @DisplayName("Given dos platos que usan huevo, When se calcula el pedido, Then el huevo se suma una sola vez")
    void shouldAggregateSharedIngredients() {
        // Arrange: 2 bandejas (2 huevos) + 1 flan (2 huevos)
        List<ItemPedido> items = List.of(new ItemPedido(BANDEJA, 2), new ItemPedido(FLAN, 1));

        // Act
        Map<String, Long> consumo = CalculadoraConsumo.calcular(items, RECETAS);

        // Assert
        assertEquals(4L, consumo.get("HUEVO"));
        assertEquals(300L, consumo.get("ARROZ"));
        assertEquals(150L, consumo.get("LECHE"));
    }

    @Test
    @DisplayName("Given un plato sin receta, When se calcula el consumo, Then no descuenta nada")
    void shouldIgnoreDishesWithoutRecipe() {
        Map<String, Long> consumo = CalculadoraConsumo.calcular(List.of(new ItemPedido(GASEOSA, 5)), RECETAS);
        assertTrue(consumo.isEmpty());
    }

    @Test
    @DisplayName("El consumo sale ordenado por código (orden fijo de bloqueo en la BD)")
    void shouldReturnSortedCodes() {
        Map<String, Long> consumo = CalculadoraConsumo.calcular(
                List.of(new ItemPedido(FLAN, 1), new ItemPedido(BANDEJA, 1)), RECETAS);
        assertEquals(List.of("ARROZ", "HUEVO", "LECHE"), List.copyOf(consumo.keySet()));
    }

    @Test
    @DisplayName("Given 7 huevos y 10.000 g de arroz, When se preguntan porciones de bandeja, Then alcanzan 7 (limita el huevo)")
    void shouldComputeAvailablePortions() {
        long porciones = CalculadoraConsumo.porcionesDisponibles(RECETA_BANDEJA,
                Map.of("HUEVO", 7L, "ARROZ", 10_000L));
        assertEquals(7L, porciones);
        assertEquals(0L, CalculadoraConsumo.porcionesDisponibles(RECETA_BANDEJA, Map.of("ARROZ", 10_000L)));
    }

    @Test
    @DisplayName("Política: en Creado se devuelve, en preparación o listo es merma, terminado no aplica")
    void shouldApplyReturnPolicy() {
        assertEquals(TipoMovimiento.DEVOLUCION, PoliticaDevolucion.tipoPara("Creado"));
        assertEquals(TipoMovimiento.MERMA, PoliticaDevolucion.tipoPara("En preparación"));
        assertEquals(TipoMovimiento.MERMA, PoliticaDevolucion.tipoPara("Listo"));
        assertThrows(IllegalArgumentException.class, () -> PoliticaDevolucion.tipoPara("Entregado"));
        assertFalse(TipoMovimiento.MERMA == PoliticaDevolucion.tipoPara("Creado"));
    }
}
