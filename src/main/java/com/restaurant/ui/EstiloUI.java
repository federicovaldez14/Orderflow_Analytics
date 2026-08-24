package com.restaurant.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Paleta y helpers de estilo compartidos por toda la interfaz, para que
 * el "look" sea consistente entre pestañas sin repetir constantes de
 * color/fuente en cada panel (evita duplicar "magia" de estilo en 5
 * archivos distintos si mañana se quiere cambiar la paleta).
 */
public final class EstiloUI {

    private EstiloUI() {
    }

    public static final Color FONDO = new Color(0xF6, 0xF1, 0xE7);           // crema cálido
    public static final Color TARJETA = Color.WHITE;
    public static final Color TEXTO_PRINCIPAL = new Color(0x3A, 0x2B, 0x1E); // espresso
    public static final Color ACENTO = new Color(0xC0, 0x54, 0x2C);          // terracota

    public static final Color LIBRE = new Color(0x4C, 0xAF, 0x50);           // verde
    public static final Color CREADO = new Color(0xF5, 0xB0, 0x41);          // ámbar
    public static final Color EN_PREPARACION = new Color(0xEB, 0x98, 0x4E);  // naranja
    public static final Color LISTO = new Color(0x5D, 0xAD, 0xE2);           // azul
    public static final Color CANCELADO = new Color(0x95, 0xA5, 0xA6);       // gris

    public static final Font FUENTE_TITULO = new Font("SansSerif", Font.BOLD, 20);
    public static final Font FUENTE_SUBTITULO = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FUENTE_TEXTO = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FUENTE_MESA = new Font("SansSerif", Font.BOLD, 20);

    /** Aplica un tema visual consistente a toda la aplicación (llamar antes de crear ventanas). */
    public static void aplicarTemaGlobal() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignorada) {
            // Si Nimbus no está disponible en el sistema, se sigue con el look and feel por defecto.
        }
        UIManager.put("control", FONDO);
        UIManager.put("info", TARJETA);
        UIManager.put("nimbusBase", ACENTO);
        UIManager.put("nimbusBlueGrey", new Color(0xE4, 0xDA, 0xC8));
        UIManager.put("text", TEXTO_PRINCIPAL);
        UIManager.put("nimbusLightBackground", TARJETA);
        UIManager.put("Table.alternateRowColor", new Color(0xFA, 0xF6, 0xEE));
        UIManager.put("defaultFont", FUENTE_TEXTO);
    }

    public static JLabel titulo(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(FUENTE_TITULO);
        label.setForeground(TEXTO_PRINCIPAL);
        return label;
    }

    public static JLabel subtitulo(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(FUENTE_SUBTITULO);
        label.setForeground(TEXTO_PRINCIPAL);
        return label;
    }

    /** Color representativo de cada estado de pedido, usado en el mapa de mesas. */
    public static Color colorParaEstado(String estado) {
        switch (estado) {
            case "Creado":
                return CREADO;
            case "En preparación":
                return EN_PREPARACION;
            case "Listo":
                return LISTO;
            case "Cancelado":
                return CANCELADO;
            default:
                return LIBRE;
        }
    }
}
