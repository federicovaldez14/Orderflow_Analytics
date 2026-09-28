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

/**
 * Prueba de SISTEMA del Reto 3: los pedidos tomados por HTTP se reflejan en
 * los indicadores y reportes que entrega la API de analítica.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"orderflow.ui.enabled=false", "orderflow.demo.historico=false", "orderflow.analitica.cache-segundos=0",
                "spring.datasource.url=jdbc:h2:mem:api_analitica_it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"})
class ApiAnaliticaIT {

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
    @DisplayName("Dos pedidos entregados y uno cancelado se reflejan en los KPI y en 'platos más pedidos'")
    void shouldReflectOrdersInAnalytics() {
        entregar(crear(1, "Bandeja Paisa", 2));    // 64.000
        entregar(crear(2, "Ajiaco santafereño", 1)); // 28.000
        int cancelado = crear(3, "Gaseosa", 5);
        rest.postForEntity("/api/pedidos/" + cancelado + "/cancelar", null, Map.class);

        ResponseEntity<Map> resp = rest.getForEntity("/api/analitica?periodo=HOY", Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<String, Object> kpi = (Map<String, Object>) resp.getBody().get("indicadores");
        assertEquals(92_000, ((Number) kpi.get("ventas")).intValue());
        assertEquals(2, ((Number) kpi.get("pedidosEntregados")).intValue());
        assertEquals(1, ((Number) kpi.get("pedidosCancelados")).intValue());

        Map<String, Object> platos = rest.getForEntity("/api/analitica/reportes/platos-mas-pedidos", Map.class).getBody();
        List<Map<String, Object>> datos = (List<Map<String, Object>>) platos.get("datos");
        assertEquals("Bandeja Paisa", datos.get(0).get("etiqueta"));
        assertEquals(2, datos.size());   // la gaseosa cancelada no cuenta
    }

    @Test
    @DisplayName("Periodo inválido -> 400; reporte inexistente -> 404")
    void shouldValidateParameters() {
        assertEquals(HttpStatus.BAD_REQUEST, rest.getForEntity("/api/analitica?periodo=AYER", Map.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, rest.getForEntity("/api/analitica/reportes/x", Map.class).getStatusCode());
    }

    private int crear(int mesa, String plato, int cantidad) {
        return (Integer) rest.postForEntity("/api/pedidos", pedido(mesa, linea(plato, cantidad)), Map.class)
                .getBody().get("id");
    }

    private void entregar(int id) {
        for (int i = 0; i < 3; i++) {
            rest.postForEntity("/api/pedidos/" + id + "/avanzar", null, Map.class);
        }
    }
}
