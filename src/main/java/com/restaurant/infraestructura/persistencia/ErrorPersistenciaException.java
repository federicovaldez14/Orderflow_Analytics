package com.restaurant.infraestructura.persistencia;

/**
 * Envuelve la SQLException para que no se filtre fuera del adaptador: el
 * núcleo no debe saber que existe JDBC. El detalle técnico va en la causa y
 * el mensaje es legible (misma idea que el taller de carga).
 */
public class ErrorPersistenciaException extends RuntimeException {
    public ErrorPersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
