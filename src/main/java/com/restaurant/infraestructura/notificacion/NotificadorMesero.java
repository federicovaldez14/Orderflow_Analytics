package com.restaurant.infraestructura.notificacion;

/** Buscapersonas del mesero: recibe todos los eventos del pedido (igual que en Corte 1). */
public class NotificadorMesero extends NotificadorEnMemoria {

    public NotificadorMesero() {
        super("MESERO");
    }
}
