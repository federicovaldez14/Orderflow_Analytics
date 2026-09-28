package com.restaurant.infraestructura.ui;

import com.restaurant.infraestructura.notificacion.NotificadorEnMemoria;

import javax.swing.*;
import java.awt.*;

/**
 * Muestra en vivo las notificaciones del patrón Observer. Cocina y mesero
 * van en columnas separadas: son dos observadores independientes
 * reaccionando al mismo evento.
 *
 * Las notificaciones pueden llegar desde hilos de peticiones HTTP; Swing
 * solo se puede tocar desde su propio hilo (EDT), por eso se usa
 * SwingUtilities.invokeLater.
 */
public class PanelNotificaciones extends JPanel {

    private static final int MAX_LINEAS = 300;

    public PanelNotificaciones(NotificadorEnMemoria cocina, NotificadorEnMemoria mesero) {
        setLayout(new GridLayout(1, 2, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(EstiloUI.FONDO);

        add(crearBloque("Pantalla de cocina", cocina));
        add(crearBloque("Buscapersonas del mesero", mesero));
    }

    private JPanel crearBloque(String titulo, NotificadorEnMemoria canal) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBackground(EstiloUI.TARJETA);
        for (String linea : canal.recientes()) {
            area.append(linea + "\n");
        }
        canal.suscribir(linea -> SwingUtilities.invokeLater(() -> agregar(area, linea)));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(EstiloUI.TARJETA);
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private static void agregar(JTextArea area, String linea) {
        area.append(linea + "\n");
        int sobrantes = area.getLineCount() - MAX_LINEAS;
        if (sobrantes > 0) {
            try {
                area.replaceRange("", 0, area.getLineEndOffset(sobrantes - 1));
            } catch (javax.swing.text.BadLocationException ignorada) {
                // si falla el recorte solo queda más texto en pantalla
            }
        }
        area.setCaretPosition(area.getDocument().getLength());
    }
}
