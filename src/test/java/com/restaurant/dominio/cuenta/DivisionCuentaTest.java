package com.restaurant.dominio.cuenta;

import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.soporte.RelojFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.restaurant.soporte.Datos.BANDEJA;
import static com.restaurant.soporte.Datos.LIMONADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reto 2 — las tres estrategias de división y el DivisorCuenta.
 * Pedido de referencia: 3x Bandeja Paisa ($96.000) + 2x Limonada ($18.000) = $114.000
 */
class DivisionCuentaTest {

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        pedido = new Pedido(20, 7, RelojFalso.el(2026, 9, 28, 14, 0));
        pedido.agregarItem(new ItemPedido(BANDEJA, 3));
        pedido.agregarItem(new ItemPedido(LIMONADA, 2));
    }

    // ---------------- Igualitaria ----------------

    @Test
    @DisplayName("Given $114.000 entre 4, When se divide en partes iguales, Then 28.500 cada uno")
    void shouldSplitEqually() {
        DivisionCuenta d = DivisorCuenta.dividir(pedido, DivisionIgualitaria.entre(4), 0);

        assertEquals(4, d.partes().size());
        d.partes().forEach(p -> assertEquals(28_500L, p.total()));
        assertEquals(114_000L, d.total());
    }

    @ParameterizedTest(name = "personas = {0}")
    @ValueSource(ints = {0, 21})
    @DisplayName("Given 0 o 21 personas (fuera de [1, 20]), When se divide, Then se rechaza")
    void shouldRejectInvalidNumberOfPeople(int n) {
        assertThrows(IllegalArgumentException.class, () -> DivisionIgualitaria.entre(n));
    }

    @Test
    @DisplayName("Given nombres repetidos o vacíos, When se divide, Then se rechaza")
    void shouldRejectInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> new DivisionIgualitaria(List.of("Ana", "ana ")));
        assertThrows(IllegalArgumentException.class, () -> new DivisionIgualitaria(List.of("Ana", " ")));
        assertThrows(IllegalArgumentException.class, () -> new DivisionIgualitaria(List.of()));
    }

    // ---------------- Por consumo ----------------

    @Test
    @DisplayName("Given Ana comió 2 bandejas y Luis 1, y compartieron las limonadas, When se divide por consumo, Then Ana 73.000 y Luis 41.000")
    void shouldSplitByConsumption() {
        // Arrange
        Map<Integer, Map<String, Integer>> asignaciones = new LinkedHashMap<>();
        asignaciones.put(0, orden("Ana", 2, "Luis", 1));      // 3x Bandeja: 64.000 / 32.000
        asignaciones.put(1, orden("Ana", 1, "Luis", 1));      // 2x Limonada: 9.000 / 9.000

        // Act
        DivisionCuenta d = DivisorCuenta.dividir(pedido, new DivisionPorConsumo(asignaciones), 0);

        // Assert
        assertEquals("Ana", d.partes().get(0).persona());
        assertEquals(73_000L, d.partes().get(0).total());
        assertEquals(41_000L, d.partes().get(1).total());
        assertTrue(d.partes().get(0).detalle().get(0).contains("Bandeja"));
    }

    @Test
    @DisplayName("Given una línea sin asignar, When se divide por consumo, Then se rechaza (no cuadraría)")
    void shouldRejectUnassignedLine() {
        Map<Integer, Map<String, Integer>> asignaciones = Map.of(0, Map.of("Ana", 1));
        EstrategiaDivision e = new DivisionPorConsumo(asignaciones);
        assertThrows(IllegalArgumentException.class, () -> DivisorCuenta.dividir(pedido, e, 0));
    }

    @Test
    @DisplayName("Given una línea que no existe, When se divide por consumo, Then se rechaza")
    void shouldRejectUnknownLine() {
        Map<Integer, Map<String, Integer>> asignaciones = new LinkedHashMap<>();
        asignaciones.put(0, Map.of("Ana", 1));
        asignaciones.put(1, Map.of("Ana", 1));
        asignaciones.put(5, Map.of("Ana", 1));
        EstrategiaDivision e = new DivisionPorConsumo(asignaciones);
        assertThrows(IllegalArgumentException.class, () -> DivisorCuenta.dividir(pedido, e, 0));
    }

    @Test
    @DisplayName("Given un peso cero o una línea sin personas, When se crea la estrategia, Then se rechaza")
    void shouldRejectInvalidWeights() {
        assertThrows(IllegalArgumentException.class, () -> new DivisionPorConsumo(Map.of(0, Map.of("Ana", 0))));
        assertThrows(IllegalArgumentException.class, () -> new DivisionPorConsumo(Map.of(0, Map.of())));
        assertThrows(IllegalArgumentException.class, () -> new DivisionPorConsumo(Map.of()));
    }

    // ---------------- Por porcentaje ----------------

    @Test
    @DisplayName("Given 60/40, When se divide por porcentaje, Then 68.400 y 45.600")
    void shouldSplitByPercentage() {
        DivisionCuenta d = DivisorCuenta.dividir(pedido,
                new DivisionPorPorcentaje(orden("Ana", 60, "Luis", 40)), 0);

        assertEquals(68_400L, d.partes().get(0).total());
        assertEquals(45_600L, d.partes().get(1).total());
    }

    @Test
    @DisplayName("Given porcentajes que suman 99 o 101, o uno en 0, When se crea la estrategia, Then se rechaza")
    void shouldRejectPercentagesNotAddingTo100() {
        assertThrows(IllegalArgumentException.class, () -> new DivisionPorPorcentaje(orden("Ana", 60, "Luis", 39)));
        assertThrows(IllegalArgumentException.class, () -> new DivisionPorPorcentaje(orden("Ana", 60, "Luis", 41)));
        assertThrows(IllegalArgumentException.class, () -> new DivisionPorPorcentaje(orden("Ana", 100, "Luis", 0)));
    }

    @Test
    @DisplayName("Given 100 % para una sola persona (límite), When se divide, Then paga todo")
    void shouldAllowSinglePersonWithHundredPercent() {
        DivisionCuenta d = DivisorCuenta.dividir(pedido, new DivisionPorPorcentaje(Map.of("Ana", 100)), 0);
        assertEquals(114_000L, d.partes().get(0).total());
    }

    // ---------------- Propina y reglas del divisor ----------------

    @Test
    @DisplayName("Given propina del 10 %, When se divide por consumo, Then la propina se reparte en proporción y todo cuadra")
    void shouldDistributeTipProportionally() {
        Map<Integer, Map<String, Integer>> asignaciones = new LinkedHashMap<>();
        asignaciones.put(0, orden("Ana", 2, "Luis", 1));
        asignaciones.put(1, orden("Ana", 1, "Luis", 1));

        DivisionCuenta d = DivisorCuenta.dividir(pedido, new DivisionPorConsumo(asignaciones), 10);

        assertEquals(11_400L, d.propina());
        assertEquals(125_400L, d.total());
        assertEquals(7_300L, d.partes().get(0).propina());   // 10 % de 73.000
        assertEquals(4_100L, d.partes().get(1).propina());   // 10 % de 41.000
        assertEquals(d.total(), d.partes().stream().mapToLong(ParteCuenta::total).sum());
    }

    @ParameterizedTest(name = "propina = {0} %")
    @ValueSource(ints = {-1, 21})
    @DisplayName("Given propina fuera de [0, 20] %, When se divide, Then se rechaza")
    void shouldRejectInvalidTip(int propina) {
        assertThrows(IllegalArgumentException.class,
                () -> DivisorCuenta.dividir(pedido, DivisionIgualitaria.entre(2), propina));
    }

    @Test
    @DisplayName("Given un pedido Cancelado, When se intenta dividir, Then se rechaza")
    void shouldNotSplitCancelledOrder() {
        pedido.cancelar();
        assertThrows(IllegalStateException.class,
                () -> DivisorCuenta.dividir(pedido, DivisionIgualitaria.entre(2), 0));
    }

    @Test
    @DisplayName("Given un pedido sin consumo, When se intenta dividir, Then se rechaza")
    void shouldNotSplitEmptyOrder() {
        Pedido vacio = new Pedido(21, 1, RelojFalso.el(2026, 9, 28, 14, 0));
        assertThrows(IllegalStateException.class,
                () -> DivisorCuenta.dividir(vacio, DivisionIgualitaria.entre(2), 0));
    }

    @Test
    @DisplayName("Given una estrategia defectuosa que no cuadra, When se arma la división, Then se detecta el error")
    void shouldDetectStrategyThatDoesNotAddUp() {
        EstrategiaDivision defectuosa = new EstrategiaDivision() {
            @Override
            public String nombre() {
                return "ROTA";
            }

            @Override
            public List<ParteCuenta> dividir(Pedido p) {
                return List.of(new ParteCuenta("Ana", 1, 0, List.of()));
            }
        };
        assertThrows(IllegalStateException.class, () -> DivisorCuenta.dividir(pedido, defectuosa, 0));
    }

    private static <V> Map<String, V> orden(String k1, V v1, String k2, V v2) {
        Map<String, V> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }
}
