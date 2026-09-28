package com.restaurant.aplicacion.casodeuso;

import com.restaurant.aplicacion.excepcion.RecursoNoEncontradoException;
import com.restaurant.aplicacion.excepcion.ReglaNegocioException;
import com.restaurant.aplicacion.puerto.salida.PedidoRepositorio;
import com.restaurant.dominio.cuenta.DivisionCuenta;
import com.restaurant.dominio.cuenta.DivisionIgualitaria;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.soporte.RelojFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.restaurant.soporte.Datos.AJIACO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicioCuentaTest {

    private PedidoRepositorio pedidos;
    private ServicioCuenta servicio;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        pedidos = mock(PedidoRepositorio.class);
        servicio = new ServicioCuenta(pedidos);
        pedido = new Pedido(8, 2, RelojFalso.el(2026, 9, 28, 15, 0));
        pedido.agregarItem(new ItemPedido(AJIACO, 2));   // 56.000
    }

    @Test
    @DisplayName("Given un pedido existente, When se divide entre 2, Then cada uno paga 28.000")
    void shouldSplitExistingOrder() {
        when(pedidos.buscarPorId(8)).thenReturn(Optional.of(pedido));

        DivisionCuenta d = servicio.dividir(8, DivisionIgualitaria.entre(2), 0);

        assertEquals(28_000L, d.partes().get(0).total());
        assertEquals(8, d.pedidoId());
    }

    @Test
    @DisplayName("Given un pedido que no existe, When se divide, Then 'no encontrado'")
    void shouldFailForUnknownOrder() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.dividir(99, DivisionIgualitaria.entre(2), 0));
    }

    @Test
    @DisplayName("Given un pedido cancelado, When se divide, Then se responde como regla de negocio")
    void shouldTranslateBusinessRejection() {
        pedido.cancelar();
        when(pedidos.buscarPorId(8)).thenReturn(Optional.of(pedido));

        assertThrows(ReglaNegocioException.class, () -> servicio.dividir(8, DivisionIgualitaria.entre(2), 0));
    }
}
