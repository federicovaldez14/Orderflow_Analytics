package com.restaurant.infraestructura.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Paleta y helpers de estilo compartidos por toda la interfaz, para que
 * el "look" sea consistente entre pestañas sin repetir constantes de
 * color/fuente en cada panel (evita duplicar "magia" de estilo en 5
 * archivos distintos si mañana se quiere cambiar la paleta).
 *
 * Tema oscuro tipo POS con FlatLaf y la fuente Inter: la misma paleta que la
 * versión web (src/main/resources/static/css/app.css), para que los dos
 * adaptadores de entrada se vean como el mismo producto.
 */
public final class EstiloUI {

    static {
        // La fuente se registra antes de crear las constantes Font de abajo.
        FlatInterFont.install();
    }

    private EstiloUI() {
    }

    public static final Color FONDO = new Color(0x17, 0x18, 0x1B);           // superficie principal
    public static final Color TARJETA = new Color(0x1E, 0x20, 0x24);         // tarjetas y paneles
    public static final Color BORDE = new Color(0x2C, 0x2F, 0x35);
    public static final Color TEXTO_PRINCIPAL = new Color(0xF2, 0xF0, 0xEB);
    public static final Color TEXTO_SECUNDARIO = new Color(0xB3, 0xAF, 0xA8);
    public static final Color ACENTO = new Color(0xE0, 0x70, 0x3C);          // terracota
    public static final Color PELIGRO = new Color(0xFF, 0x8A, 0x82);

    // Colores de estado validados para daltonismo sobre la superficie oscura;
    // el estado siempre se muestra también como texto.
    public static final Color LIBRE = new Color(0x3A, 0x3D, 0x44);           // gris
    public static final Color CREADO = new Color(0xC9, 0x85, 0x00);          // ámbar
    public static final Color EN_PREPARACION = new Color(0x90, 0x85, 0xE9);  // violeta
    public static final Color LISTO = new Color(0x19, 0x9E, 0x70);           // verde
    public static final Color CANCELADO = new Color(0x55, 0x58, 0x5F);       // gris

    public static final Font FUENTE_TITULO = new Font(FlatInterFont.FAMILY, Font.BOLD, 22);
    public static final Font FUENTE_SUBTITULO = new Font(FlatInterFont.FAMILY_SEMIBOLD, Font.PLAIN, 14);
    public static final Font FUENTE_TEXTO = new Font(FlatInterFont.FAMILY, Font.PLAIN, 13);
    public static final Font FUENTE_MESA = new Font(FlatInterFont.FAMILY, Font.BOLD, 20);

    /** Aplica un tema visual consistente a toda la aplicación (llamar antes de crear ventanas). */
    public static void aplicarTemaGlobal() {
        FlatLaf.setGlobalExtraDefaults(Map.of(
                "@accentColor", "#e0703c",
                "@background", "#17181b"));
        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
        FlatDarkLaf.setup();

        UIManager.put("defaultFont", FUENTE_TEXTO);
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("TabbedPane.tabHeight", 42);
        UIManager.put("TabbedPane.selectedBackground", TARJETA);
        UIManager.put("TabbedPane.underlineColor", ACENTO);
        UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.gridColor", BORDE);
        UIManager.put("Table.alternateRowColor", new Color(0x1B, 0x1D, 0x20));
        UIManager.put("TitlePane.unifiedBackground", true);
        FlatLaf.updateUI();
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

    /**
     * Ejecuta una acción de la UI y, si el núcleo rechaza la operación (regla
     * de negocio, dato inválido), muestra el mensaje en vez de dejar que la
     * excepción se pierda en la consola. Las reglas viven en el dominio; la UI
     * solo muestra lo que el dominio responde.
     */
    public static boolean intentar(java.awt.Component padre, Runnable accion) {
        try {
            accion.run();
            return true;
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(padre, e.getMessage(), "No se pudo completar",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    public static String pesos(long valor) {
        return String.format("$%,d", valor);
    }
}
