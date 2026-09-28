package com.restaurant.aplicacion.excepcion;

/**
 * Una operación válida en forma pero que el negocio no permite en el estado
 * actual (mesa ocupada, stock insuficiente, cuenta que no cuadra...).
 * El adaptador REST la traduce a HTTP 409 Conflict.
 */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
