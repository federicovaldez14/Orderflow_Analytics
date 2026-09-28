package com.restaurant.infraestructura.ui;

import com.restaurant.aplicacion.casodeuso.GestorPedidos;
import com.restaurant.aplicacion.casodeuso.ServicioAnalitica;
import com.restaurant.aplicacion.casodeuso.ServicioCuenta;
import com.restaurant.aplicacion.casodeuso.ServicioInventario;
import com.restaurant.infraestructura.notificacion.AlertasInventarioEnMemoria;
import com.restaurant.infraestructura.notificacion.NotificadorEnMemoria;

import javax.swing.*;
import java.awt.*;

/**
 * Ventana principal del POS de escritorio (adaptador de entrada Swing).
 *
 * Recibe los casos de uso ya construidos por Spring (ver LanzadorUI): la UI
 * no crea repositorios ni decide la base de datos. Si alguien crea un pedido
 * por la API REST, esta ventana lo muestra en el siguiente refresco
 * automático (cada 2 s), porque ambos adaptadores comparten el mismo núcleo.
 */
public class PosApp extends JFrame {

    private final JTabbedPane tabs = new JTabbedPane();

    public PosApp(GestorPedidos gestor, ServicioInventario inventario, ServicioCuenta cuenta,
                  ServicioAnalitica analitica,
                  AlertasInventarioEnMemoria alertas,
                  NotificadorEnMemoria cocina, NotificadorEnMemoria mesero) {
        super("Orderflow Analytics — POS Restaurante — Corte 2");

        PanelMapaMesas[] mapaRef = new PanelMapaMesas[1];
        PanelPedidosActivos[] listaRef = new PanelPedidosActivos[1];
        Runnable refrescarLista = () -> {
            if (listaRef[0] != null) listaRef[0].refrescar();
        };
        Runnable refrescarMapa = () -> {
            if (mapaRef[0] != null) mapaRef[0].refrescar();
        };

        PanelMapaMesas.AccionesCuenta dividir = (padre, pedidoId) -> EstiloUI.intentar(padre, () ->
                new DialogoDividirCuenta(SwingUtilities.getWindowAncestor(padre), gestor, cuenta, pedidoId)
                        .setVisible(true));
        PanelMapaMesas panelMapa = new PanelMapaMesas(gestor, refrescarLista, dividir);
        PanelPedidosActivos panelPedidos = new PanelPedidosActivos(gestor, refrescarMapa, dividir);
        mapaRef[0] = panelMapa;
        listaRef[0] = panelPedidos;

        tabs.setFont(EstiloUI.FUENTE_SUBTITULO);
        tabs.addTab("Mapa de mesas", panelMapa);
        tabs.addTab("Lista de pedidos", panelPedidos);
        tabs.addTab("Inventario", new PanelInventario(inventario, alertas));
        tabs.addTab("Notificaciones", new PanelNotificaciones(cocina, mesero));
        tabs.addTab("Analítica", new PanelAnalitica(analitica));
        tabs.addChangeListener(e -> refrescarVisible());

        // Refresco periódico: muestra cambios hechos por otros adaptadores (REST).
        new Timer(2000, e -> refrescarVisible()).start();

        setContentPane(tabs);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 700);
        setMinimumSize(new Dimension(950, 580));
        setLocationRelativeTo(null);
    }

    private void refrescarVisible() {
        Component visible = tabs.getSelectedComponent();
        if (visible instanceof PanelMapaMesas) {
            ((PanelMapaMesas) visible).refrescar();
        } else if (visible instanceof PanelPedidosActivos) {
            ((PanelPedidosActivos) visible).refrescar();
        } else if (visible instanceof PanelAnalitica) {
            ((PanelAnalitica) visible).refrescar();
        } else if (visible instanceof PanelInventario) {
            ((PanelInventario) visible).refrescar();
        }
    }
}
