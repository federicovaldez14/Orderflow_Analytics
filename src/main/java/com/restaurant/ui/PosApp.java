package com.restaurant.ui;

import com.restaurant.observador.Notificador;
import com.restaurant.servicio.GestorPedidos;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Punto de entrada de la interfaz gráfica: un mini POS de escritorio
 * (Swing) con pestañas para el mapa de mesas, la lista de pedidos, las
 * notificaciones en vivo y la analítica.
 *
 * Esta clase NO modifica ninguna clase de modelo/estado/fabrica/
 * observador/reportes/servicio salvo Pedido (que ahora soporta editar
 * ítems, ver Pedido.agregarItem/removerItem/estaEditable). Reutiliza
 * GestorPedidos, PlatoFactory (vía MenuRestaurante), EstadoPedido,
 * Notificador y EstrategiaReporte tal como están.
 *
 * El "Mapa de mesas" y la "Lista de pedidos" leen y escriben sobre el
 * mismo GestorPedidos; por eso cada acción que modifica datos en una
 * pestaña dispara un refresco cruzado en la otra (ver los Runnable
 * onCambio que se pasan a los paneles).
 */
public class PosApp extends JFrame {

    public PosApp() {
        super("Restaurante — Sistema de Pedidos (POS) — Corte 1");

        PanelNotificaciones panelNotificaciones = new PanelNotificaciones();
        List<Notificador> observadores = Arrays.asList(
                new NotificadorUI(panelNotificaciones.getAreaCocina(), "COCINA"),
                new NotificadorUI(panelNotificaciones.getAreaMesero(), "MESERO")
        );
        GestorPedidos gestor = new GestorPedidos(observadores);

        // Se crean primero como referencias mutables para poder
        // pasarse callbacks de refresco cruzado entre sí.
        PanelMapaMesas[] mapaRef = new PanelMapaMesas[1];
        PanelPedidosActivos[] listaRef = new PanelPedidosActivos[1];

        Runnable refrescarLista = () -> {
            if (listaRef[0] != null) listaRef[0].refrescar();
        };
        Runnable refrescarMapa = () -> {
            if (mapaRef[0] != null) mapaRef[0].refrescar();
        };

        PanelMapaMesas panelMapa = new PanelMapaMesas(gestor, refrescarLista);
        PanelPedidosActivos panelPedidos = new PanelPedidosActivos(gestor, refrescarMapa);
        mapaRef[0] = panelMapa;
        listaRef[0] = panelPedidos;

        PanelAnalitica panelAnalitica = new PanelAnalitica(gestor);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(EstiloUI.FUENTE_SUBTITULO);
        tabs.addTab("Mapa de mesas", panelMapa);
        tabs.addTab("Lista de pedidos", panelPedidos);
        tabs.addTab("Notificaciones", panelNotificaciones);
        tabs.addTab("Analítica", panelAnalitica);
        // Al entrar a cada pestaña, refresca por si hubo cambios en otra.
        tabs.addChangeListener(e -> {
            panelMapa.refrescar();
            panelPedidos.refrescar();
        });

        setContentPane(tabs);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 650);
        setMinimumSize(new java.awt.Dimension(900, 550));
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EstiloUI.aplicarTemaGlobal();
            new PosApp().setVisible(true);
        });
    }
}
