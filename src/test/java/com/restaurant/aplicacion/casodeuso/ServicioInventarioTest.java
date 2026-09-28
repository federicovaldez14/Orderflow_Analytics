package com.restaurant.aplicacion.casodeuso;

import com.restaurant.aplicacion.excepcion.RecursoNoEncontradoException;
import com.restaurant.aplicacion.excepcion.ReglaNegocioException;
import com.restaurant.aplicacion.puerto.salida.AlertaInventario;
import com.restaurant.aplicacion.puerto.salida.InventarioRepositorio;
import com.restaurant.aplicacion.puerto.salida.InventarioRepositorio.Motivo;
import com.restaurant.dominio.inventario.Ingrediente;
import com.restaurant.dominio.inventario.Receta;
import com.restaurant.dominio.inventario.TipoMovimiento;
import com.restaurant.dominio.inventario.UnidadMedida;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.soporte.RelojFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.restaurant.soporte.Datos.BANDEJA;
import static com.restaurant.soporte.Datos.GASEOSA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Caso de uso del inventario aislado: el repositorio y la alerta son dobles
 * de prueba. Se verifica QUÉ le pide el caso de uso a los puertos (cuánto
 * descontar, con qué motivo) sin tocar una base de datos.
 */
class ServicioInventarioTest {

    private InventarioRepositorio repo;
    private AlertaInventario alerta;
    private ServicioInventario servicio;
    private RelojFalso reloj;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        repo = mock(InventarioRepositorio.class);
        alerta = mock(AlertaInventario.class);
        reloj = RelojFalso.el(2026, 9, 28, 20, 0);
        servicio = new ServicioInventario(repo, List.of(alerta), reloj);
        pedido = new Pedido(11, 3, reloj);

        when(repo.recetasPorPlato()).thenReturn(Map.of("Bandeja Paisa",
                new Receta("Bandeja Paisa", Map.of("ARROZ", 150L, "HUEVO", 1L))));
    }

    @Test
    @DisplayName("Given 2 bandejas, When se reserva, Then se descuenta 300 g de arroz y 2 huevos como SALIDA_VENTA del pedido")
    void shouldDiscountRecipeTimesQuantity() {
        // Arrange
        ArgumentCaptor<Map> consumo = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Motivo> motivo = ArgumentCaptor.forClass(Motivo.class);
        when(repo.descontar(anyMap(), any())).thenReturn(Map.of("ARROZ", 9_700L, "HUEVO", 58L));

        // Act
        servicio.reservar(pedido, List.of(new ItemPedido(BANDEJA, 2)));

        // Assert
        verify(repo).descontar(consumo.capture(), motivo.capture());
        assertEquals(300L, consumo.getValue().get("ARROZ"));
        assertEquals(2L, consumo.getValue().get("HUEVO"));
        assertEquals(TipoMovimiento.SALIDA_VENTA, motivo.getValue().tipo());
        assertEquals(11, motivo.getValue().pedidoId());
    }

    @Test
    @DisplayName("Given platos sin receta, When se reserva, Then no se toca el inventario")
    void shouldSkipDishesWithoutRecipe() {
        servicio.reservar(pedido, List.of(new ItemPedido(GASEOSA, 1)));
        verify(repo, never()).descontar(anyMap(), any());
    }

    @Test
    @DisplayName("Given el huevo pasa de 5 a 3 con mínimo 4, When se reserva, Then se emite UNA alerta de stock bajo")
    void shouldAlertWhenCrossingMinimum() {
        // Arrange
        Ingrediente huevo = new Ingrediente("HUEVO", "Huevo", UnidadMedida.UNIDAD, 3, 4);
        Ingrediente arroz = new Ingrediente("ARROZ", "Arroz", UnidadMedida.GRAMO, 9_700, 1_000);
        when(repo.descontar(anyMap(), any())).thenReturn(Map.of("ARROZ", 9_700L, "HUEVO", 3L));
        when(repo.buscarIngrediente("HUEVO")).thenReturn(Optional.of(huevo));
        when(repo.buscarIngrediente("ARROZ")).thenReturn(Optional.of(arroz));

        // Act
        servicio.reservar(pedido, List.of(new ItemPedido(BANDEJA, 2)));

        // Assert
        verify(alerta).stockBajo(huevo);
        verify(alerta, never()).stockBajo(arroz);
    }

    @Test
    @DisplayName("Given el pedido seguía Creado, When se liberan platos, Then vuelven al stock (DEVOLUCION)")
    void shouldReturnIngredientsWhenOrderWasCreated() {
        ArgumentCaptor<Motivo> motivo = ArgumentCaptor.forClass(Motivo.class);

        servicio.liberar(pedido, List.of(new ItemPedido(BANDEJA, 1)), "Creado");

        verify(repo).sumar(anyMap(), motivo.capture());
        assertEquals(TipoMovimiento.DEVOLUCION, motivo.getValue().tipo());
        verify(repo, never()).registrarSinCambio(anyMap(), any());
    }

    @Test
    @DisplayName("Given el pedido estaba en preparación, When se liberan platos, Then se registra MERMA sin devolver stock")
    void shouldRegisterWasteWhenOrderWasInPreparation() {
        ArgumentCaptor<Motivo> motivo = ArgumentCaptor.forClass(Motivo.class);

        servicio.liberar(pedido, List.of(new ItemPedido(BANDEJA, 1)), "En preparación");

        verify(repo).registrarSinCambio(anyMap(), motivo.capture());
        assertEquals(TipoMovimiento.MERMA, motivo.getValue().tipo());
        verify(repo, never()).sumar(anyMap(), any());
    }

    @Test
    @DisplayName("Given 500 g de reposición, When se repone, Then se registra una ENTRADA sin pedido")
    void shouldRestock() {
        Ingrediente arroz = new Ingrediente("ARROZ", "Arroz", UnidadMedida.GRAMO, 100, 1_000);
        when(repo.buscarIngrediente("ARROZ")).thenReturn(Optional.of(arroz));
        ArgumentCaptor<Motivo> motivo = ArgumentCaptor.forClass(Motivo.class);

        servicio.reponer("arroz", 500, "Compra plaza");

        verify(repo).sumar(anyMap(), motivo.capture());
        assertEquals(TipoMovimiento.ENTRADA, motivo.getValue().tipo());
        assertEquals(null, motivo.getValue().pedidoId());
        assertEquals("Compra plaza", motivo.getValue().descripcion());
    }

    @Test
    @DisplayName("Given un ingrediente inexistente o cantidad no positiva, When se repone, Then se rechaza")
    void shouldValidateRestock() {
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.reponer("UNICORNIO", 5, null));
        when(repo.buscarIngrediente("ARROZ")).thenReturn(Optional.of(
                new Ingrediente("ARROZ", "Arroz", UnidadMedida.GRAMO, 100, 10)));
        assertThrows(IllegalArgumentException.class, () -> servicio.reponer("ARROZ", 0, null));
    }

    @Test
    @DisplayName("Given el sistema dice 100 y el conteo físico 80, When se ajusta, Then se descuentan 20 como AJUSTE")
    void shouldAdjustDownwards() {
        when(repo.buscarIngrediente("ARROZ")).thenReturn(Optional.of(
                new Ingrediente("ARROZ", "Arroz", UnidadMedida.GRAMO, 100, 10)));
        when(repo.descontar(anyMap(), any())).thenReturn(Map.of("ARROZ", 80L));
        ArgumentCaptor<Map> cantidades = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Motivo> motivo = ArgumentCaptor.forClass(Motivo.class);

        servicio.ajustarConteo("ARROZ", 80, null);

        verify(repo).descontar(cantidades.capture(), motivo.capture());
        assertEquals(20L, cantidades.getValue().get("ARROZ"));
        assertEquals(TipoMovimiento.AJUSTE, motivo.getValue().tipo());
    }

    @Test
    @DisplayName("Given el conteo coincide con el sistema, When se ajusta, Then se responde que no hay nada que ajustar")
    void shouldRejectNoOpAdjustment() {
        when(repo.buscarIngrediente("ARROZ")).thenReturn(Optional.of(
                new Ingrediente("ARROZ", "Arroz", UnidadMedida.GRAMO, 100, 10)));
        assertThrows(ReglaNegocioException.class, () -> servicio.ajustarConteo("ARROZ", 100, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.ajustarConteo("ARROZ", -1, null));
    }

    @Test
    @DisplayName("Given un límite fuera de [1, 1000], When se consultan movimientos, Then se rechaza")
    void shouldValidateMovementLimit() {
        assertThrows(IllegalArgumentException.class, () -> servicio.movimientos(null, 0));
        assertThrows(IllegalArgumentException.class, () -> servicio.movimientos(null, 1001));
        servicio.movimientos(" arroz ", 1000);
        verify(repo).movimientos("ARROZ", 1000);
        verifyNoInteractions(alerta);
    }

    @Test
    @DisplayName("Porciones disponibles se calculan con el stock actual y la receta")
    void shouldComputeAvailablePortions() {
        when(repo.listarIngredientes()).thenReturn(List.of(
                new Ingrediente("ARROZ", "Arroz", UnidadMedida.GRAMO, 1_000, 10),
                new Ingrediente("HUEVO", "Huevo", UnidadMedida.UNIDAD, 20, 4)));

        assertEquals(6L, servicio.porcionesDisponibles().get("Bandeja Paisa"));
    }
}
