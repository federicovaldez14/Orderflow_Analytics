package com.restaurant.ui;

import com.restaurant.modelo.Pedido;
import com.restaurant.observador.Notificador;

import javax.swing.JTextArea;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementación de Notificador (patrón Observer) que escribe en un
 * JTextArea en vez de en consola. Pedido no sabe ni le importa la
 * diferencia: sigue llamando a notificar(pedido, mensaje) exactamente
 * igual que con NotificadorCocina/NotificadorMesero de la versión de
 * consola. Esa es la prueba de que el canal de salida es intercambiable.
 */
public class NotificadorUI implements Notificador {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextArea area;
    private final String etiqueta;

    public NotificadorUI(JTextArea area, String etiqueta) {
        this.area = area;
        this.etiqueta = etiqueta;
    }

    @Override
    public void notificar(Pedido pedido, String mensaje) {
        String linea = String.format("[%s] %s | Pedido #%d (mesa %d): %s%n",
                LocalTime.now().format(HORA), etiqueta, pedido.getId(), pedido.getMesa(), mensaje);
        area.append(linea);
        area.setCaretPosition(area.getDocument().getLength());
    }
}
