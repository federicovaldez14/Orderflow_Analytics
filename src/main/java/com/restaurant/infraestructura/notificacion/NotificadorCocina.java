package com.restaurant.infraestructura.notificacion;

/** Pantalla de cocina: recibe todos los eventos del pedido (igual que en Corte 1). */
public class NotificadorCocina extends NotificadorEnMemoria {

    public NotificadorCocina() {
        super("COCINA");
    }
}
