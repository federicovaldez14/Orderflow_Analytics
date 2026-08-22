package com.restaurant.ui;

import com.restaurant.observador.Notificador;
import com.restaurant.servicio.GestorPedidos;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Punto de entrada de la interfaz gráfica: un mini POS de escritorio
 * (Swing) con pestañas para crear pedidos, verlos/gestionarlos,
 * observar notificaciones en vivo y consultar analítica.
 *
 * Esta clase NO modifica ninguna clase de modelo/estado/fabrica/
 * observador/reportes/servicio ya entregada: es una capa nueva
 * (interfaz gráfica) construida encima del mismo dominio de la versión
 * de consola. Reutiliza GestorPedidos, PlatoFactory (vía MenuRestaurante),
 * EstadoPedido, Notificador y EstrategiaReporte tal como están.
 */
public class PosApp extends JFrame {

    public PosApp() {
        super("Restaurante - Sistema de Pedidos (POS) - Corte 1");

        PanelNotificaciones panelNotificaciones = new PanelNotificaciones();
        List<Notificador> observadores = Arrays.asList(
                new NotificadorUI(panelNotificaciones.getAreaCocina(), "COCINA"),
                new NotificadorUI(panelNotificaciones.getAreaMesero(), "MESERO")
        );
        GestorPedidos gestor = new GestorPedidos(observadores);

        PanelPedidosActivos panelPedidos = new PanelPedidosActivos(gestor);
        PanelAnalitica panelAnalitica = new PanelAnalitica(gestor);
        PanelNuevoPedido panelNuevoPedido = new PanelNuevoPedido(gestor, panelPedidos::refrescar);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Nuevo pedido", panelNuevoPedido);
        tabs.addTab("Pedidos activos", panelPedidos);
        tabs.addTab("Notificaciones", panelNotificaciones);
        tabs.addTab("Analítica", panelAnalitica);

        setContentPane(tabs);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 620);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PosApp().setVisible(true));
    }
}
