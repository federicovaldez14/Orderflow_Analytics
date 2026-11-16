package com.restaurant.infraestructura.seguridad;

import com.restaurant.infraestructura.config.DatosIniciales;
import com.restaurant.soporte.Credenciales;
import com.restaurant.soporte.LimpiadorBD;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.restaurant.soporte.Json.linea;
import static com.restaurant.soporte.Json.mapa;
import static com.restaurant.soporte.Json.pedido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de SISTEMA de las mitigaciones del modelo de amenazas
 * (docs/seguridad.md): autenticación, autorización por rol, CSRF para el
 * navegador y endpoints de salud y métricas. Aplicación completa por HTTP.
 */
@AutoConfigureObservability   // en pruebas Spring Boot apaga /actuator/prometheus si no se pide
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {Credenciales.PROPIEDAD_CLAVE_ADMIN, "orderflow.ui.enabled=false", "orderflow.demo.historico=false",
                "spring.datasource.url=jdbc:h2:mem:seguridad_it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"})
class SeguridadIT {

    private static final String CLAVE = "clave-de-rol-12345";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatosIniciales datosIniciales;

    private TestRestTemplate admin;

    @BeforeEach
    void preparar() {
        LimpiadorBD.limpiar(dataSource, datosIniciales);
        admin = rest.withBasicAuth(Credenciales.ADMIN, Credenciales.CLAVE_ADMIN);
        for (String rol : List.of("MESERO", "COCINA", "MONITOR")) {
            String usuario = rol.toLowerCase();
            if (admin.getForEntity("/api/usuarios", List.class).getBody().stream()
                    .noneMatch(u -> usuario.equals(((Map<?, ?>) u).get("usuario")))) {
                admin.postForEntity("/api/usuarios", mapa("usuario", usuario, "clave", CLAVE, "rol", rol), Map.class);
            }
        }
    }

    private TestRestTemplate como(String usuario) {
        return rest.withBasicAuth(usuario, CLAVE);
    }

    @Test
    @DisplayName("Spoofing: sin credenciales la API responde 401 y no pide usuario al navegador (sin WWW-Authenticate); "
            + "un POST anónimo sin token CSRF se rechaza antes (403)")
    void shouldRejectAnonymousRequests() {
        ResponseEntity<String> resp = rest.getForEntity("/api/pedidos", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertFalse(resp.getHeaders().containsKey(HttpHeaders.WWW_AUTHENTICATE));
        assertEquals(HttpStatus.FORBIDDEN,
                rest.postForEntity("/api/pedidos", pedido(1, linea("Gaseosa", 1)), String.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, rest.withBasicAuth("nadie", "clave-inventada-1")
                .postForEntity("/api/pedidos", pedido(1, linea("Gaseosa", 1)), String.class).getStatusCode());
    }

    @Test
    @DisplayName("Spoofing: clave incorrecta -> 401")
    void shouldRejectWrongPassword() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                rest.withBasicAuth(Credenciales.ADMIN, "otra-clave-cualquiera").getForEntity("/api/pedidos", String.class)
                        .getStatusCode());
    }

    @Test
    @DisplayName("Elevación de privilegios: un mesero no puede ajustar inventario ni ver la analítica (403); el admin sí")
    void shouldRestrictInventoryAndAnalyticsToAdmin() {
        Object ajuste = mapa("stockContado", 3, "nota", "conteo");

        assertEquals(HttpStatus.FORBIDDEN,
                como("mesero").postForEntity("/api/inventario/AGUACATE/ajuste", ajuste, String.class).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, como("mesero").getForEntity("/api/analitica", String.class).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, como("mesero").getForEntity("/api/usuarios", String.class).getStatusCode());
        assertEquals(HttpStatus.OK, admin.postForEntity("/api/inventario/AGUACATE/ajuste", ajuste, String.class)
                .getStatusCode());
    }

    @Test
    @DisplayName("Elevación de privilegios: cocina avanza comandas pero no las crea ni las cancela; el mesero sí las crea")
    void shouldLetKitchenOnlyAdvanceOrders() {
        assertEquals(HttpStatus.FORBIDDEN,
                como("cocina").postForEntity("/api/pedidos", pedido(2, linea("Gaseosa", 1)), String.class).getStatusCode());

        ResponseEntity<Map> creado = como("mesero").postForEntity("/api/pedidos", pedido(2, linea("Gaseosa", 1)), Map.class);
        int id = (Integer) creado.getBody().get("id");

        assertEquals(HttpStatus.CREATED, creado.getStatusCode());
        assertEquals(HttpStatus.OK, como("cocina").postForEntity("/api/pedidos/" + id + "/avanzar", null, String.class)
                .getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, como("cocina").postForEntity("/api/pedidos/" + id + "/cancelar", null, String.class)
                .getStatusCode());
    }

    @Test
    @DisplayName("Divulgación: las métricas solo las lee MONITOR o ADMIN; la salud es pública y sin detalles")
    void shouldProtectMetricsButNotHealth() {
        ResponseEntity<String> salud = rest.getForEntity("/actuator/health/readiness", String.class);

        assertEquals(HttpStatus.OK, salud.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, rest.getForEntity("/actuator/prometheus", String.class).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, como("mesero").getForEntity("/actuator/prometheus", String.class).getStatusCode());
        assertEquals(HttpStatus.OK, como("monitor").getForEntity("/actuator/prometheus", String.class).getStatusCode());
        assertFalse(rest.getForEntity("/actuator/health", String.class).getBody().contains("db"));
    }

    @Test
    @DisplayName("CSRF: en el navegador, iniciar sesión y crear un pedido exigen el token; con él funcionan")
    void shouldRequireCsrfTokenForBrowserSessions() {
        // Arrange: la primera visita deja la cookie XSRF-TOKEN
        ResponseEntity<String> visita = rest.getForEntity("/api/sesion", String.class);
        String xsrf = cookie(visita, "XSRF-TOKEN").orElseThrow();

        // Act + Assert: login sin token -> 403; con token -> 200 y cookie de sesión
        assertEquals(HttpStatus.FORBIDDEN, login(null, null).getStatusCode());
        ResponseEntity<String> login = login(xsrf, "XSRF-TOKEN=" + xsrf);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        assertTrue(login.getBody().contains("\"ADMIN\""));
        String sesion = cookie(login, "JSESSIONID").orElseThrow();
        String xsrfNuevo = cookie(login, "XSRF-TOKEN").orElse(xsrf);

        // Con la sesión: POST sin token -> 403; con token -> 201
        assertEquals(HttpStatus.FORBIDDEN, postConSesion(sesion, xsrfNuevo, false).getStatusCode());
        assertEquals(HttpStatus.CREATED, postConSesion(sesion, xsrfNuevo, true).getStatusCode());
    }

    @Test
    @DisplayName("Solo el admin crea usuarios; claves cortas o roles inválidos se rechazan (400)")
    void shouldValidateNewUsers() {
        assertEquals(HttpStatus.BAD_REQUEST, admin.postForEntity("/api/usuarios",
                mapa("usuario", "corto", "clave", "123", "rol", "MESERO"), String.class).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, admin.postForEntity("/api/usuarios",
                mapa("usuario", "nuevo.usuario", "clave", CLAVE, "rol", "DUENO"), String.class).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, como("mesero").postForEntity("/api/usuarios",
                mapa("usuario", "intruso", "clave", CLAVE, "rol", "ADMIN"), String.class).getStatusCode());
    }

    private ResponseEntity<String> login(String xsrf, String cookie) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (xsrf != null) {
            h.add("X-XSRF-TOKEN", xsrf);
            h.add(HttpHeaders.COOKIE, cookie);
        }
        String cuerpo = "usuario=" + Credenciales.ADMIN + "&clave=" + Credenciales.CLAVE_ADMIN;
        return rest.exchange("/api/sesion/login", HttpMethod.POST, new HttpEntity<>(cuerpo, h), String.class);
    }

    private ResponseEntity<String> postConSesion(String sesion, String xsrf, boolean conToken) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.add(HttpHeaders.COOKIE, "JSESSIONID=" + sesion + "; XSRF-TOKEN=" + xsrf);
        if (conToken) {
            h.add("X-XSRF-TOKEN", xsrf);
        }
        return rest.exchange("/api/pedidos", HttpMethod.POST, new HttpEntity<>(pedido(7, linea("Gaseosa", 1)), h),
                String.class);
    }

    private static Optional<String> cookie(ResponseEntity<?> resp, String nombre) {
        return Optional.ofNullable(resp.getHeaders().get(HttpHeaders.SET_COOKIE)).orElse(List.of()).stream()
                .filter(c -> c.startsWith(nombre + "="))
                .map(c -> c.substring(nombre.length() + 1, c.indexOf(';') < 0 ? c.length() : c.indexOf(';')))
                .filter(v -> !v.isEmpty())
                .findFirst();
    }
}
