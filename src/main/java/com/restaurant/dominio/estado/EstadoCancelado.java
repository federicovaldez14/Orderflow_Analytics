package com.restaurant.dominio.estado;

import com.restaurant.dominio.modelo.Pedido;

/**
 * Estado terminal alternativo al flujo normal: el pedido se cancela
 * (el cliente se fue, se acabó un insumo, error del mesero, etc.).
 * No se llega aquí mediante avanzar() como los demás estados — se llega
 * explícitamente con Pedido.cancelar(), que valida que el pedido no esté
 * ya en un estado terminal.
 *
 * Es la prueba de extensibilidad del patrón State: esta clase se agregó
 * sin modificar Pedido.java ni ningún otro EstadoXxx existente.
 */
public class EstadoCancelado implements EstadoPedido {

    @Override
    public String getNombre() {
        return "Cancelado";
    }

    @Override
    public void avanzar(Pedido pedido) {
        // Terminal: un pedido cancelado no avanza a ningún otro estado.
    }

    @Override
    public boolean esTerminal() {
        return true;
    }
}
