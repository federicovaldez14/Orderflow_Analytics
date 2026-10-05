package com.restaurant.uitests;

import com.restaurant.infraestructura.config.DatosIniciales;
import com.restaurant.soporte.LimpiadorBD;
import com.restaurant.uitests.paginas.DialogoDivision;
import com.restaurant.uitests.paginas.PaginaCocina;
import com.restaurant.uitests.paginas.PaginaInventario;
import com.restaurant.uitests.paginas.PaginaSalon;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static com.restaurant.soporte.Json.linea;
import static com.restaurant.soporte.Json.pedido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PRUEBAS DE UI (bonificación): Selenium sobre el POS web, con la aplicación
 * real levantada en un puerto aleatorio (Spring Boot + H2 + REST + estáticos).
 *
 * Page Object Model: cada pantalla es una clase en uitests/paginas y las
 * pruebas solo hablan en términos del negocio. Esperas explícitas en los
 * Page Objects (sin Thread.sleep). Cada prueba parte de la base limpia.
 *
 * Comando: mvn verify -Pui-tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"orderflow.ui.enabled=false", "orderflow.demo.historico=false", "orderflow.analitica.cache-segundos=0",
                "spring.datasource.url=jdbc:h2:mem:pos_web_ui_it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"})
class PosWebUiIT {

    private static WebDriver driver;

    @LocalServerPort
    private int puerto;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatosIniciales datosIniciales;

    private String url;

    @BeforeAll
    static void abrirNavegador() {
        driver = Navegador.crear();
    }

    @AfterAll
    static void cerrarNavegador() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void limpiar() {
        LimpiadorBD.limpiar(dataSource, datosIniciales);
        url = "http://localhost:" + puerto;
        // Página en blanco primero: así cada prueba recarga la web desde cero (sin diálogos ni estado de la anterior)
        driver.get("about:blank");
        driver.get(url + "/#/salon");
    }

    @Test
    @DisplayName("Flujo 1 · El mesero toma la comanda de la mesa 3 y la cocina la lleva hasta entregarla")
    void shouldTakeOrderAndDeliverItThroughKitchen() {
        // Arrange
        PaginaSalon salon = new PaginaSalon(driver, url).abrir();
        PaginaCocina cocina = new PaginaCocina(driver, url);

        // Act: tomar la comanda en el salón
        salon.seleccionarMesa(3)
                .agregarPlato("Bandeja Paisa", 2)
                .agregarPlato("Limonada de coco", 1)
                .enviarACocina();
        String aviso = salon.esperarAviso("enviada a cocina");
        salon.esperarEstadoMesa(3, "Creado");
        String total = salon.totalDelPanel();

        // Act: la cocina la avanza por cada estado
        cocina.abrir()
                .esperarComanda(3, "Creado").avanzar(3)
                .esperarComanda(3, "En preparación").avanzar(3)
                .esperarComanda(3, "Listo").avanzar(3)
                .esperarSinComanda(3);

        // Assert: la comanda salió con el total correcto y la mesa quedó libre
        assertTrue(aviso.contains("mesa 3"), aviso);
        assertTrue(total.contains("73.000"), total);   // 2 x 32.000 + 9.000
        new PaginaSalon(driver, url).abrir().esperarEstadoMesa(3, "Libre");
    }

    @Test
    @DisplayName("Flujo 2 · La cuenta de la mesa 5 se divide por consumo, con un plato compartido, y cuadra al peso")
    void shouldSplitBillByConsumptionAndMatchTotal() {
        // Arrange: pedido de 87.000 creado por la API (2 Bandeja, 1 Limonada, 1 Patacones)
        rest.postForEntity("/api/pedidos", pedido(5, linea("Bandeja Paisa", 2), linea("Limonada de coco", 1),
                linea("Patacones con hogao", 1)), Map.class);
        PaginaSalon salon = new PaginaSalon(driver, url).abrir().seleccionarMesa(5);

        // Act: Ana se comió las bandejas, Luis tomó la limonada y compartieron los patacones; 10 % de propina
        DialogoDivision division = salon.abrirDivisionDeCuenta()
                .elegirMetodo("POR_CONSUMO")
                .nombrarPersona(0, "Ana")
                .nombrarPersona(1, "Luis")
                .partes(0, 1, 0)
                .partes(1, 0, 0)
                .propina(10);
        String verificacion = division.calcular();
        List<String> partes = division.partesDelResultado();

        // Assert: 87.000 + 8.700 de propina = 95.700, y la suma de las partes da exactamente eso
        assertTrue(verificacion.contains("95.700"), verificacion);
        assertTrue(verificacion.contains("suma de las partes $ 95.700"), verificacion);
        assertEquals(2, partes.size());
        assertTrue(partes.get(0).contains("Ana") && partes.get(0).contains("78.100"), partes.get(0));   // 71.000 + 7.100
        assertTrue(partes.get(1).contains("Luis") && partes.get(1).contains("17.600"), partes.get(1));  // 16.000 + 1.600
    }

    @Test
    @DisplayName("Flujo 3 · Un conteo físico deja el aguacate bajo el mínimo y la web muestra la alerta")
    void shouldShowLowStockAlertAfterPhysicalCount() {
        // Arrange
        PaginaInventario inventario = new PaginaInventario(driver, url).abrir();

        // Act
        inventario.ajustarPorConteo("AGUACATE", 3);
        String aviso = inventario.esperarAviso("stock actualizado");
        inventario.esperarAlertaBajoMinimo("AGUACATE");

        // Assert
        assertTrue(aviso.contains("Aguacate"), aviso);
        assertEquals("3 und", inventario.stockMostrado("AGUACATE"));
        assertEquals("1", inventario.insigniaDeAlertas());
    }

    @Test
    @DisplayName("Mejora UX verificada (H5-1) · Cambiar de mesa no borra la comanda que se estaba armando")
    void shouldKeepUnsentOrderWhenSwitchingTables() {
        // Arrange: el mesero empieza la comanda de la mesa 2
        PaginaSalon salon = new PaginaSalon(driver, url).abrir()
                .seleccionarMesa(2)
                .agregarPlato("Gaseosa", 2);

        // Act: lo llaman de la mesa 7 y luego vuelve a la 2
        salon.seleccionarMesa(7);
        salon.esperarBorradorEnMesa(2);
        salon.seleccionarMesa(2);

        // Assert: las dos gaseosas siguen ahí, listas para enviar
        assertEquals(2, salon.unidadesEnComanda("Gaseosa"));
    }
}
