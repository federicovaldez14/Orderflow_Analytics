package com.restaurant.infraestructura.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Administración de usuarios (solo ADMIN, ver ConfiguracionSeguridad):
 * crear meseros, cocineros y el usuario de monitoreo, y listarlos.
 * Las claves se guardan con BCrypt; nunca se devuelven.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[a-zA-Z0-9._-]{3,50}");

    public record NuevoUsuarioDto(String usuario, String clave, String rol) {
    }

    public record UsuarioDto(String usuario, String rol, boolean activo) {
    }

    private final JdbcUserDetailsManager usuarios;
    private final PasswordEncoder codificador;
    private final JdbcTemplate jdbc;

    public UsuarioController(JdbcUserDetailsManager usuarios, PasswordEncoder codificador, DataSource dataSource) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return jdbc.query("SELECT u.username, a.authority, u.enabled FROM users u "
                        + "JOIN authorities a ON a.username = u.username ORDER BY u.username",
                (rs, i) -> new UsuarioDto(rs.getString(1), rs.getString(2).replace("ROLE_", ""), rs.getBoolean(3)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@RequestBody NuevoUsuarioDto nuevo) {
        if (nuevo.usuario() == null || !NOMBRE_VALIDO.matcher(nuevo.usuario()).matches()) {
            throw new IllegalArgumentException("El usuario debe tener de 3 a 50 letras, números, '.', '_' o '-'");
        }
        if (nuevo.clave() == null || nuevo.clave().length() < UsuariosIniciales.LONGITUD_MINIMA_CLAVE) {
            throw new IllegalArgumentException("La clave debe tener al menos "
                    + UsuariosIniciales.LONGITUD_MINIMA_CLAVE + " caracteres");
        }
        Rol rol;
        try {
            rol = Rol.valueOf(nuevo.rol() == null ? "" : nuevo.rol().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido (use ADMIN, MESERO, COCINA o MONITOR)");
        }
        if (usuarios.userExists(nuevo.usuario())) {
            throw new IllegalStateException("El usuario '" + nuevo.usuario() + "' ya existe");
        }
        usuarios.createUser(User.withUsername(nuevo.usuario())
                .password(codificador.encode(nuevo.clave()))
                .roles(rol.name())
                .build());
        return new UsuarioDto(nuevo.usuario(), rol.name(), true);
    }
}
