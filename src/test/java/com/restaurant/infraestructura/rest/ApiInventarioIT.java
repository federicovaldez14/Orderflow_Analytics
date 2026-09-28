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
import static com.restaurant.soporte.Json.mapa;
import static com.restaurant.soporte.Json.pedido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de SISTEMA del Reto 1 por HTTP (caja negra): pedidos e inventario
 * funcionando juntos con la base real embebida.
 *
 * Receta de la Bandeja Paisa: 1 huevo, 150 g de arroz, ... (DatosIniciales).
 * Stock inicial de HUEVO: 60.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"orderflow.ui.enabled=false", "orderflow.demo.historico=false", "orderflow.analitica.cache-segundos=0",
                "spring.datasource.url=jdbc:h2:mem:api_inventario_it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"})
class ApiInventarioIT {

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
    @DisplayName("Tomar 2 bandejas descuenta 2 huevos y 300 g de arroz, y el pedido queda trazado")
    void shouldDiscountInventoryWhenOrdering() {
        // Arrange
        long huevosAntes = stock("HUEVO");
        long arrozAntes = stock("ARROZ");

        // Act
        int id = crearPedido(1, "Bandeja Paisa", 2);

        // Assert
        assertEquals(huevosAntes - 2, stock("HUEVO"));
        assertEquals(arrozAntes - 300, stock("ARROZ"));
        List<Map<String, Object>> movs = rest.getForEntity("/api/pedidos/" + id + "/inventario", List.class).getBody();
        assertEquals(7, movs.size());   // la bandeja tiene 7 ingredientes
        assertTrue(movs.stream().allMatch(m -> "SALIDA_VENTA".equals(m.get("tipo"))));
    }

    @Test
    @DisplayName("Cancelar un pedido que seguía Creado devuelve los ingredientes (DEVOLUCION)")
    void shouldReturnIngredientsWhenCancellingCreatedOrder() {
        long huevosAntes = stock("HUEVO");
        int id = crearPedido(2, "Bandeja Paisa", 3);

        rest.postForEntity("/api/pedidos/" + id + "/cancelar", null, Map.class);

        assertEquals(huevosAntes, stock("HUEVO"));
        List<Map<String, Object>> movs = rest.getForEntity("/api/pedidos/" + id + "/inventario", List.class).getBody();
        assertTrue(movs.stream().anyMatch(m -> "DEVOLUCION".equals(m.get("tipo"))));
    }

    @Test
    @DisplayName("Cancelar un pedido en preparación registra MERMA y no devuelve stock")
    void shouldRegisterWasteWhenCancellingInPreparation() {
        long huevosAntes = stock("HUEVO");
        int id = crearPedido(3, "Bandeja Paisa", 1);
        rest.postForEntity("/api/pedidos/" + id + "/avanzar", null, Map.class);

        rest.postForEntity("/api/pedidos/" + id + "/cancelar", null, Map.class);

        assertEquals(huevosAntes - 1, stock("HUEVO"));
        List<Map<String, Object>> movs = rest.getForEntity("/api/pedidos/" + id + "/inventario", List.class).getBody();
        assertTrue(movs.stream().anyMatch(m -> "MERMA".equals(m.get("tipo"))));
    }

    @Test
    @DisplayName("Sin huevos: el pedido responde 409 con el faltante, no se crea y la mesa sigue libre")
    void shouldRejectOrderWithoutStock() {
        // Arrange: conteo físico dice que no hay huevos
        rest.postForEntity("/api/inventario/HUEVO/ajuste", mapa("stockContado", 0, "nota", "se rompieron"), Map.class);

        // Act
        ResponseEntity<Map> resp = rest.postForEntity("/api/pedidos",
                pedido(4, linea("Bandeja Paisa", 1)), Map.class);

        // Assert
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        List<Map<String, Object>> faltantes = (List<Map<String, Object>>) resp.getBody().get("faltantes");
        assertEquals("HUEVO", faltantes.get(0).get("codigo"));
        List<Map<String, Object>> mesas = rest.getForEntity("/api/mesas", List.class).getBody();
        assertEquals(true, mesas.get(3).get("libre"));
        assertEquals(0, ((List<?>) rest.getForEntity("/api/pedidos", List.class).getBody()).size());
    }

    @Test
    @DisplayName("Reponer suma al stock y aparece como ENTRADA en el historial del ingrediente")
    void shouldRestockAndTrace() {
        long antes = stock("CAFE");

        ResponseEntity<Map> resp = rest.postForEntity("/api/inventario/cafe/reposicion",
                mapa("cantidad", 500, "nota", "Compra Juan Valdez"), Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(antes + 500, stock("CAFE"));
        List<Map<String, Object>> movs = rest.getForEntity("/api/inventario/movimientos?ingrediente=CAFE",
                List.class).getBody();
        assertEquals("ENTRADA", movs.get(0).get("tipo"));
    }

    @Test
    @DisplayName("Bajar un ingrediente del mínimo lo lista en /api/inventario/alertas")
    void shouldListLowStockAlerts() {
        rest.postForEntity("/api/inventario/CAFE/ajuste", mapa("stockContado", 50, "nota", "conteo"), Map.class);

        List<Map<String, Object>> alertas = rest.getForEntity("/api/inventario/alertas", List.class).getBody();

        assertTrue(alertas.stream().anyMatch(a -> "CAFE".equals(a.get("codigo"))));
    }

    private int crearPedido(int mesa, String plato, int cantidad) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/pedidos", pedido(mesa, linea(plato, cantidad)), Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return (Integer) resp.getBody().get("id");
    }

    private long stock(String codigo) {
        List<Map<String, Object>> inv = rest.getForEntity("/api/inventario", List.class).getBody();
        return inv.stream().filter(i -> codigo.equals(i.get("codigo")))
                .map(i -> ((Number) i.get("stock")).longValue()).findFirst().orElseThrow();
    }
}
