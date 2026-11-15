package com.restaurant.infraestructura.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;

/**
 * Crea las tablas de usuarios (esquema estándar de Spring Security, igual en
 * H2 y PostgreSQL) y, si todavía no hay ningún usuario, el primer administrador
 * con las variables ORDERFLOW_ADMIN_USUARIO y ORDERFLOW_ADMIN_CLAVE.
 *
 * No hay claves en el código: si ORDERFLOW_ADMIN_CLAVE no está definida (por
 * ejemplo, al correr en local con mvn spring-boot:run) se genera una clave
 * aleatoria y se muestra una sola vez en el log, como hace Spring Boot.
 */
@Component
@Order(0)
public class UsuariosIniciales implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(UsuariosIniciales.class);
    static final int LONGITUD_MINIMA_CLAVE = 12;

    private final DataSource dataSource;
    private final JdbcUserDetailsManager usuarios;
    private final PasswordEncoder codificador;
    private final String adminUsuario;
    private final String adminClave;

    public UsuariosIniciales(DataSource dataSource, JdbcUserDetailsManager usuarios, PasswordEncoder codificador,
                             @Value("${orderflow.seguridad.admin-usuario:admin}") String adminUsuario,
                             @Value("${orderflow.seguridad.admin-clave:}") String adminClave) {
        this.dataSource = dataSource;
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.adminUsuario = adminUsuario;
        this.adminClave = adminClave;
    }

    @Override
    public void run(ApplicationArguments args) {
        crearEsquema();
        if (hayUsuarios()) {
            return;
        }
        String clave = adminClave;
        if (clave == null || clave.isBlank()) {
            clave = claveAleatoria();
            LOG.warn("ORDERFLOW_ADMIN_CLAVE no está definida. Clave generada para '{}' (solo se muestra una vez): {}",
                    adminUsuario, clave);
        } else if (clave.length() < LONGITUD_MINIMA_CLAVE) {
            throw new IllegalStateException("ORDERFLOW_ADMIN_CLAVE debe tener al menos "
                    + LONGITUD_MINIMA_CLAVE + " caracteres");
        }
        usuarios.createUser(User.withUsername(adminUsuario)
                .password(codificador.encode(clave))
                .roles(Rol.ADMIN.name())
                .build());
        LOG.info("Usuario administrador inicial '{}' creado", adminUsuario);
    }

    private void crearEsquema() {
        try (Connection con = dataSource.getConnection(); Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users(" +
                    " username VARCHAR(50) NOT NULL PRIMARY KEY," +
                    " password VARCHAR(500) NOT NULL," +
                    " enabled BOOLEAN NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS authorities(" +
                    " username VARCHAR(50) NOT NULL," +
                    " authority VARCHAR(50) NOT NULL," +
                    " CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users(username))");
            st.execute("CREATE UNIQUE INDEX IF NOT EXISTS ix_auth_username ON authorities(username, authority)");
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear el esquema de usuarios", e);
        }
    }

    private boolean hayUsuarios() {
        try (Connection con = dataSource.getConnection(); Statement st = con.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            return rs.getLong(1) > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer la tabla de usuarios", e);
        }
    }

    private static String claveAleatoria() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
