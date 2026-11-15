package com.restaurant.infraestructura.seguridad;

/**
 * Roles del restaurante. Cada usuario tiene uno; Spring Security lo guarda
 * como la autoridad "ROLE_" + nombre.
 */
public enum Rol {
    /** Dueño o administrador: inventario, analítica y usuarios, además de todo lo demás. */
    ADMIN,
    /** Toma comandas, edita pedidos, cancela, entrega y divide la cuenta. */
    MESERO,
    /** Ve las comandas y las avanza (en preparación, listo). */
    COCINA,
    /** Solo lee /actuator/prometheus (lo usa Prometheus para la observabilidad). */
    MONITOR;

    public String autoridad() {
        return "ROLE_" + name();
    }
}
