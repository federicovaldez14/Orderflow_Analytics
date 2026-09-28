package com.restaurant.infraestructura.ui;

import com.restaurant.aplicacion.casodeuso.GestorPedidos;
import com.restaurant.aplicacion.casodeuso.ServicioCuenta;
import com.restaurant.aplicacion.casodeuso.ServicioInventario;
import com.restaurant.infraestructura.notificacion.AlertasInventarioEnMemoria;
import com.restaurant.infraestructura.notificacion.NotificadorCocina;
import com.restaurant.infraestructura.notificacion.NotificadorMesero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/**
 * Abre el POS de escritorio cuando arranca la aplicación, con los MISMOS
 * casos de uso que atiende la API REST.
 *
 * Se desactiva con orderflow.ui.enabled=false (pruebas automáticas y de
 * carga) y también si la máquina no tiene pantalla (servidor, CI).
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "orderflow.ui.enabled", havingValue = "true")
public class LanzadorUI implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(LanzadorUI.class);

    private final GestorPedidos gestor;
    private final ServicioInventario inventario;
    private final ServicioCuenta cuenta;
    private final AlertasInventarioEnMemoria alertas;
    private final NotificadorCocina cocina;
    private final NotificadorMesero mesero;

    public LanzadorUI(GestorPedidos gestor, ServicioInventario inventario, ServicioCuenta cuenta,
                      AlertasInventarioEnMemoria alertas,
                      NotificadorCocina cocina, NotificadorMesero mesero) {
        this.gestor = gestor;
        this.inventario = inventario;
        this.cuenta = cuenta;
        this.alertas = alertas;
        this.cocina = cocina;
        this.mesero = mesero;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (GraphicsEnvironment.isHeadless()) {
            LOG.warn("Sin pantalla disponible: solo se levanta la API REST en /api");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            EstiloUI.aplicarTemaGlobal();
            new PosApp(gestor, inventario, cuenta, alertas, cocina, mesero).setVisible(true);
        });
    }
}
