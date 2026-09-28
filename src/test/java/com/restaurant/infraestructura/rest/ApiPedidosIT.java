package com.restaurant.infraestructura.rest;

import com.restaurant.infraestructura.config.DatosIniciales;
import com.restaurant.soporte.LimpiadorBD;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static com.restaurant.soporte.Json.linea;
import static com.restaurant.soporte.Json.pedido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de SISTEMA de caja negra: levanta la aplicación completa (Spring
 * Boot + H2 + adaptadores reales) en un puerto aleatorio y la usa solo por
 * HTTP, como lo haría un cliente o k6. No mira el código interno.
 *
 * Aislamiento: cada prueba empieza con las tablas de pedidos vacías
 * (LimpiadorBD), así no dependen del orden de ejecución.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"orderflow.ui.enabled=false", "orderflow.demo.historico=false",
                "spring.datasource.url=jdbc:h2:mem:api_pedidos_it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"})
class ApiPedidosIT {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatosIniciales datosIniciales;

    @BeforeEach
    void limpiar() {
        LimpiadorBD.limpiar(dataSource, datosIniciales);
    }

    @Test
    @DisplayName("GET /api/menu devuelve la carta cargada al arrancar")
    void shouldExposeMenu() {
        ResponseEntity<List> resp = rest.getForEntity("/api/menu", List.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(10, resp.getBody().size());
    }

    @Test
    @DisplayName("Flujo completo por HTTP: crear -> avanzar x3 -> Entregado, y la mesa queda libre")
    void shouldRunFullOrderFlowOverHttp() {
        // Arrange + Act: crear
        ResponseEntity<Map> creado = rest.postForEntity("/api/pedidos",
                pedido(3, linea("Bandeja Paisa", 2), linea("Limonada de coco", 2)), Map.class);

        // Assert: 201 con total correcto
        assertEquals(HttpStatus.CREATED, creado.getStatusCode());
        int id = (Integer) creado.getBody().get("id");
        assertEquals(82_000, ((Number) creado.getBody().get("total")).intValue());
        assertEquals("Creado", creado.getBody().get("estado"));

        // Act: avanzar tres veces
        String estado = null;
        for (int i = 0; i < 3; i++) {
            estado = (String) rest.postForEntity("/api/pedidos/" + id + "/avanzar", null, Map.class)
                    .getBody().get("estado");
        }

        // Assert
        assertEquals("Entregado", estado);
        List<Map<String, Object>> mesas = rest.getForEntity("/api/mesas", List.class).getBody();
        assertEquals(true, mesas.get(2).get("libre"));
    }

    @Test
    @DisplayName("Una segunda comanda para la misma mesa ocupada responde 409 Conflict")
    void shouldReturnConflictForOccupiedTable() {
        rest.postForEntity("/api/pedidos", pedido(5, linea("Gaseosa", 1)), Map.class);

        ResponseEntity<Map> segundo = rest.postForEntity("/api/pedidos", pedido(5, linea("Gaseosa", 1)), Map.class);

        assertEquals(HttpStatus.CONFLICT, segundo.getStatusCode());
        assertTrue(((String) segundo.getBody().get("mensaje")).contains("mesa 5"));
    }

    @Test
    @DisplayName("Datos inválidos responden 400 y recursos inexistentes 404")
    void shouldMapErrorsToHttpCodes() {
        assertEquals(HttpStatus.BAD_REQUEST,
                rest.postForEntity("/api/pedidos", pedido(99, linea("Gaseosa", 1)), Map.class).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                rest.postForEntity("/api/pedidos", pedido(1, linea("Gaseosa", 0)), Map.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                rest.postForEntity("/api/pedidos", pedido(1, linea("Pizza", 1)), Map.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                rest.getForEntity("/api/pedidos/12345", Map.class).getStatusCode());
    }

    @Test
    @DisplayName("Cancelar dos veces el mismo pedido: la segunda responde 409")
    void shouldNotCancelTwice() {
        int id = (Integer) rest.postForEntity("/api/pedidos", pedido(2, linea("Gaseosa", 1)), Map.class)
                .getBody().get("id");

        assertEquals(HttpStatus.OK,
                rest.postForEntity("/api/pedidos/" + id + "/cancelar", null, Map.class).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                rest.postForEntity("/api/pedidos/" + id + "/cancelar", null, Map.class).getStatusCode());
    }
}
