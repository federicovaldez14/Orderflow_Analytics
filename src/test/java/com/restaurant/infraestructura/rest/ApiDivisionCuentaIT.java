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
import static com.restaurant.soporte.Json.lista;
import static com.restaurant.soporte.Json.mapa;
import static com.restaurant.soporte.Json.pedido;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Prueba de SISTEMA del Reto 2 por HTTP: se toma un pedido real, se entrega
 * y se divide la cuenta de las tres formas. Siempre se verifica que la suma
 * de lo que paga cada persona sea exactamente el total.
 *
 * Pedido: 3x Bandeja Paisa (96.000) + 2x Limonada de coco (18.000) = 114.000
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"orderflow.ui.enabled=false", "orderflow.demo.historico=false", "orderflow.analitica.cache-segundos=0",
                "spring.datasource.url=jdbc:h2:mem:api_division_it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"})
class ApiDivisionCuentaIT {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatosIniciales datosIniciales;

    private int pedidoId;

    @BeforeEach
    void prepararPedidoEntregado() {
        LimpiadorBD.limpiar(dataSource, datosIniciales);
        pedidoId = (Integer) rest.postForEntity("/api/pedidos",
                pedido(6, linea("Bandeja Paisa", 3), linea("Limonada de coco", 2)), Map.class).getBody().get("id");
        for (int i = 0; i < 3; i++) {
            rest.postForEntity("/api/pedidos/" + pedidoId + "/avanzar", null, Map.class);
        }
    }

    @Test
    @DisplayName("POST division IGUALITARIA entre 3 con propina 10 %: cuadra al peso")
    void shouldSplitEquallyWithTip() {
        ResponseEntity<Map> resp = dividir(mapa("metodo", "IGUALITARIA", "numeroPersonas", 3, "propinaPorcentaje", 10));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(125_400, numero(resp.getBody().get("total")));
        List<Map<String, Object>> partes = (List<Map<String, Object>>) resp.getBody().get("partes");
        assertEquals(3, partes.size());
        assertEquals(125_400, partes.stream().mapToLong(p -> numero(p.get("total"))).sum());
    }

    @Test
    @DisplayName("POST division POR_CONSUMO: Ana 2 bandejas, Luis 1, limonadas a medias")
    void shouldSplitByConsumption() {
        ResponseEntity<Map> resp = dividir(mapa("metodo", "POR_CONSUMO", "asignaciones", lista(
                mapa("linea", 0, "personas", mapa("Ana", 2, "Luis", 1)),
                mapa("linea", 1, "personas", mapa("Ana", 1, "Luis", 1)))));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> partes = (List<Map<String, Object>>) resp.getBody().get("partes");
        assertEquals("Ana", partes.get(0).get("persona"));
        assertEquals(73_000, numero(partes.get(0).get("total")));
        assertEquals(41_000, numero(partes.get(1).get("total")));
    }

    @Test
    @DisplayName("POST division POR_PORCENTAJE 70/30")
    void shouldSplitByPercentage() {
        ResponseEntity<Map> resp = dividir(mapa("metodo", "POR_PORCENTAJE",
                "porcentajes", mapa("Ana", 70, "Luis", 30)));

        List<Map<String, Object>> partes = (List<Map<String, Object>>) resp.getBody().get("partes");
        assertEquals(79_800, numero(partes.get(0).get("total")));
        assertEquals(34_200, numero(partes.get(1).get("total")));
    }

    @Test
    @DisplayName("Datos que no cuadran -> 400; método desconocido -> 400; pedido cancelado -> 409")
    void shouldRejectInvalidRequests() {
        assertEquals(HttpStatus.BAD_REQUEST, dividir(mapa("metodo", "POR_PORCENTAJE",
                "porcentajes", mapa("Ana", 70, "Luis", 20))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, dividir(mapa("metodo", "POR_CONSUMO", "asignaciones", lista(
                mapa("linea", 0, "personas", mapa("Ana", 1))))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, dividir(mapa("metodo", "A_LA_SUERTE")).getStatusCode());

        int otro = (Integer) rest.postForEntity("/api/pedidos", pedido(7, linea("Gaseosa", 1)), Map.class)
                .getBody().get("id");
        rest.postForEntity("/api/pedidos/" + otro + "/cancelar", null, Map.class);
        assertEquals(HttpStatus.CONFLICT, rest.postForEntity("/api/pedidos/" + otro + "/division",
                mapa("metodo", "IGUALITARIA", "numeroPersonas", 2), Map.class).getStatusCode());
    }

    private ResponseEntity<Map> dividir(Map<String, Object> cuerpo) {
        return rest.postForEntity("/api/pedidos/" + pedidoId + "/division", cuerpo, Map.class);
    }

    private static long numero(Object o) {
        return ((Number) o).longValue();
    }
}
