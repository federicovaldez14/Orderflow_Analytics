package com.restaurant.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Panel de solo lectura donde se ven en vivo las notificaciones que el
 * patrón Observer dispara cada vez que un pedido cambia de estado.
 * Cocina y mesero se muestran en columnas separadas para que se note que
 * son dos observadores independientes reaccionando al mismo evento.
 */
public class PanelNotificaciones extends JPanel {

    private final JTextArea areaCocina = new JTextArea();
    private final JTextArea areaMesero = new JTextArea();

    public PanelNotificaciones() {
        setLayout(new GridLayout(1, 2, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(EstiloUI.FONDO);

        add(crearBloque("Pantalla de cocina", areaCocina));
        add(crearBloque("Buscapersonas del mesero", areaMesero));
    }

    private JPanel crearBloque(String titulo, JTextArea area) {
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBackground(EstiloUI.TARJETA);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(EstiloUI.TARJETA);
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    public JTextArea getAreaCocina() {
        return areaCocina;
    }

    public JTextArea getAreaMesero() {
        return areaMesero;
    }
}
