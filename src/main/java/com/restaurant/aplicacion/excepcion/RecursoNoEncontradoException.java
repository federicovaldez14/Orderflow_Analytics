package com.restaurant.aplicacion.excepcion;

/** Se pidió algo que no existe (pedido, plato, ingrediente). REST: 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
