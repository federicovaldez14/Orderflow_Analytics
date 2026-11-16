package com.restaurant.infraestructura.seguridad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Inyección en el log (CWE-117): un valor con saltos de línea no puede fabricar otra entrada de auditoría. */
class FiltroAuditoriaTest {

    @Test
    void los_saltos_de_linea_no_parten_la_entrada_de_auditoria() {
        String ataque = "/api/pedidos\r\n2026-11-15 INFO AUDITORIA : usuario=admin POST /api/usuarios -> 201";
        String limpio = FiltroAuditoria.sinSaltos(ataque);
        assertEquals(1, limpio.lines().count());
        assertEquals("/api/pedidos__2026-11-15 INFO AUDITORIA : usuario=admin POST /api/usuarios -> 201", limpio);
    }

    @Test
    void un_valor_nulo_queda_vacio() {
        assertEquals("", FiltroAuditoria.sinSaltos(null));
    }
}
