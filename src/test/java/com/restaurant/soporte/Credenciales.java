package com.restaurant.soporte;

/**
 * Credenciales que usan las pruebas de sistema. Solo existen en la base H2 en
 * memoria de cada prueba (se crean al arrancar con la propiedad de abajo); no
 * son las de ningún entorno real.
 */
public final class Credenciales {

    private Credenciales() {
    }

    public static final String ADMIN = "admin";
    public static final String CLAVE_ADMIN = "clave-solo-pruebas-1";
    /** Para @SpringBootTest(properties = ...): fija la clave del administrador inicial. */
    public static final String PROPIEDAD_CLAVE_ADMIN = "orderflow.seguridad.admin-clave=" + CLAVE_ADMIN;
}
