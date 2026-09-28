package com.restaurant.soporte;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Deja la base como recién arrancada antes de cada prueba de sistema:
 * sin pedidos, sin movimientos y con el inventario inicial (DatosIniciales).
 * La carta (tabla plato) se conserva porque es un dato maestro.
 */
public final class LimpiadorBD {

    private LimpiadorBD() {
    }

    public static void limpiar(DataSource ds, com.restaurant.infraestructura.config.DatosIniciales datos) {
        try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM item_pedido");
            st.executeUpdate("DELETE FROM pedido");
            st.executeUpdate("DELETE FROM movimiento_inventario");
            st.executeUpdate("DELETE FROM receta");
            st.executeUpdate("DELETE FROM ingrediente");
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo limpiar la base de pruebas", e);
        }
        datos.cargarInventario();
    }
}
