package com.restaurant.infraestructura.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Adaptador de seguridad (Spring Security). Autenticación y autorización por
 * rol para la API REST y el POS web; el núcleo hexagonal no sabe nada de esto.
 *
 * - Navegador: inicia sesión en POST /api/sesion/login y queda con una cookie
 *   de sesión. Toda petición que cambia datos lleva el token CSRF (cookie
 *   XSRF-TOKEN -> cabecera X-XSRF-TOKEN).
 * - Scripts y pruebas (curl, prueba de humo, pruebas de integración): HTTP Basic.
 *   Las peticiones con cabecera Authorization no usan cookies y no son
 *   vulnerables a CSRF, por eso no lo exigen. Para que el navegador nunca
 *   guarde credenciales Basic, un 401 no envía WWW-Authenticate (no aparece
 *   el cuadro de usuario y clave del navegador).
 * - Las claves se guardan con BCrypt (DelegatingPasswordEncoder).
 *
 * Reglas de autorización: docs/seguridad.md (modelo de amenazas).
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private static final String ADMIN = Rol.ADMIN.name();
    private static final String MESERO = Rol.MESERO.name();
    private static final String COCINA = Rol.COCINA.name();
    private static final String MONITOR = Rol.MONITOR.name();

    @Bean
    public SecurityFilterChain seguridad(HttpSecurity http) throws Exception {
        HttpStatusEntryPoint sin401Basico = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

        http
            .authorizeHttpRequests(auth -> auth
                // Archivos de la web y salud para los healthchecks: públicos (no contienen datos)
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                .requestMatchers("/actuator/prometheus").hasAnyRole(MONITOR, ADMIN)
                .requestMatchers("/actuator/**").hasRole(ADMIN)
                .requestMatchers("/error").permitAll()
                // Sesión
                .requestMatchers("/api/sesion", "/api/sesion/**").permitAll()
                // Administración
                .requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole(ADMIN)
                .requestMatchers("/api/analitica", "/api/analitica/**").hasRole(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/inventario/**").hasRole(ADMIN)
                // Flujo de pedidos
                .requestMatchers(HttpMethod.POST, "/api/pedidos/*/avanzar").hasAnyRole(ADMIN, MESERO, COCINA)
                .requestMatchers(HttpMethod.POST, "/api/pedidos/**").hasAnyRole(ADMIN, MESERO)
                .requestMatchers(HttpMethod.DELETE, "/api/pedidos/**").hasAnyRole(ADMIN, MESERO)
                // Lecturas: cualquier usuario del restaurante (no el de métricas)
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(ADMIN, MESERO, COCINA)
                .anyRequest().denyAll())
            .formLogin(form -> form
                .loginProcessingUrl("/api/sesion/login")
                .usernameParameter("usuario")
                .passwordParameter("clave")
                .successHandler((req, res, auth) -> escribirSesion(res, HttpStatus.OK, auth))
                .failureHandler((req, res, ex) ->
                        escribirError(res, HttpStatus.UNAUTHORIZED, "Usuario o clave incorrectos")))
            .logout(logout -> logout
                .logoutUrl("/api/sesion/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value()))
                .deleteCookies("JSESSIONID"))
            .httpBasic(basic -> basic.authenticationEntryPoint(sin401Basico))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(sin401Basico)
                .accessDeniedHandler((req, res, e) -> escribirError(res, HttpStatus.FORBIDDEN,
                        e instanceof CsrfException
                                ? "Falta el token de seguridad o la sesión venció; recargue la página"
                                : "Su rol no permite esta acción")))
            .csrf(csrf -> csrf
                .csrfTokenRepository(repositorioCsrf())
                .csrfTokenRequestHandler(new ManejadorCsrfWeb())
                .requireCsrfProtectionMatcher(cambiaDatosDesdeNavegador()))
            .addFilterAfter(new FiltroAuditoria(), BasicAuthenticationFilter.class)
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self'; connect-src 'self'; img-src 'self' data:; "
                                + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                + "font-src 'self' https://fonts.gstatic.com; frame-ancestors 'none'; "
                                + "base-uri 'self'; form-action 'self'"))
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }

    /**
     * CSRF solo para peticiones del navegador que cambian datos: las que llegan
     * con Authorization (HTTP Basic) no dependen de cookies y no son atacables por CSRF.
     */
    static RequestMatcher cambiaDatosDesdeNavegador() {
        return req -> !("GET".equals(req.getMethod()) || "HEAD".equals(req.getMethod())
                || "OPTIONS".equals(req.getMethod()) || "TRACE".equals(req.getMethod()))
                && req.getHeader("Authorization") == null;
    }

    /** Cookie XSRF-TOKEN legible por la web (su único fin es copiarse en la cabecera), solo en el mismo sitio. */
    private static CookieCsrfTokenRepository repositorioCsrf() {
        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookieCustomizer(c -> c.sameSite("Strict").path("/"));
        return repo;
    }

    @Bean
    public PasswordEncoder codificadorClaves() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public JdbcUserDetailsManager usuarios(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    private static void escribirSesion(jakarta.servlet.http.HttpServletResponse res, HttpStatus status,
                                       Authentication auth) throws IOException {
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> "\"" + a.replace("ROLE_", "") + "\"")
                .collect(Collectors.joining(","));
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"usuario\":\"" + Json.escapar(auth.getName()) + "\",\"roles\":[" + roles + "]}");
    }

    static void escribirError(jakarta.servlet.http.HttpServletResponse res, HttpStatus status,
                              String mensaje) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"mensaje\":\"" + Json.escapar(mensaje) + "\"}");
    }

    /** Escapado mínimo para escribir texto dentro de un string JSON. */
    static final class Json {
        private Json() {
        }

        static String escapar(String texto) {
            StringBuilder sb = new StringBuilder();
            for (char c : texto.toCharArray()) {
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '<': sb.append("\\u003c"); break;
                    case '>': sb.append("\\u003e"); break;
                    default:
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                }
            }
            return sb.toString();
        }
    }
}
