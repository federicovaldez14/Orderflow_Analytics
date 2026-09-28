package com.restaurant.soporte;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.util.UUID;

/**
 * Crea una base H2 en memoria NUEVA para cada prueba (nombre aleatorio).
 * Así las pruebas de integración no comparten datos ni dependen del orden
 * de ejecución (requisito del enunciado, sección 3.2).
 */
public final class BaseDeDatosH2 {

    private BaseDeDatosH2() {
    }

    public static DataSource nueva() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:it_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }
}
