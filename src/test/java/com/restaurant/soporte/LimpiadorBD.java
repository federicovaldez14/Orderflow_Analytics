package com.restaurant.soporte;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Deja las tablas de movimiento vacías antes de cada prueba de sistema.
 * La carta (tabla plato) se conserva porque es un dato maestro que se
 * carga al arrancar.
 */
public final class LimpiadorBD {

    private LimpiadorBD() {
    }

    public static void limpiar(DataSource ds) {
        try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM item_pedido");
            st.executeUpdate("DELETE FROM pedido");
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo limpiar la base de pruebas", e);
        }
    }
}
