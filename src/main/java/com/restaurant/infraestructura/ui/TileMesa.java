package com.restaurant.infraestructura.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Un "tile" clicable que representa una mesa dentro del mapa del salón.
 * Se dibuja a mano (en vez de usar un JButton estándar) para poder
 * colorearla según su estado y darle esquinas redondeadas, más cercano a
 * la estética de un POS real.
 */
public class TileMesa extends JPanel {

    private final int numeroMesa;
    private Color colorEstado = EstiloUI.LIBRE;
    private String subtitulo = "Libre";
    private boolean seleccionada = false;

    public TileMesa(int numeroMesa, Consumer<Integer> alHacerClick) {
        this.numeroMesa = numeroMesa;
        setPreferredSize(new Dimension(140, 100));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                alHacerClick.accept(numeroMesa);
            }
        });
    }

    /** Actualiza cómo se ve la mesa sin reconstruir el componente. */
    public void actualizar(Color colorEstado, String subtitulo, boolean seleccionada) {
        this.colorEstado = colorEstado;
        this.subtitulo = subtitulo;
        this.seleccionada = seleccionada;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        boolean libre = colorEstado.equals(EstiloUI.LIBRE);
        Shape tarjeta = new java.awt.geom.RoundRectangle2D.Float(4, 4, w - 8, h - 8, 16, 16);

        // Tarjeta oscura con una franja superior del color del estado (como en la web).
        g2.setColor(EstiloUI.TARJETA);
        g2.fill(tarjeta);
        if (!libre) {
            Shape recorte = g2.getClip();
            g2.clip(tarjeta);
            g2.setColor(colorEstado);
            g2.fillRect(4, 4, w - 8, 4);
            g2.setClip(recorte);
        }
        g2.setColor(seleccionada ? EstiloUI.ACENTO : EstiloUI.BORDE);
        g2.setStroke(new BasicStroke(seleccionada ? 2f : 1f));
        g2.draw(tarjeta);

        int x = 18;
        g2.setFont(EstiloUI.FUENTE_TEXTO.deriveFont(Font.BOLD, 10f));
        g2.setColor(EstiloUI.TEXTO_SECUNDARIO);
        g2.drawString("MESA", x, 28);

        g2.setFont(EstiloUI.FUENTE_MESA.deriveFont(28f));
        g2.setColor(libre ? EstiloUI.TEXTO_SECUNDARIO : EstiloUI.TEXTO_PRINCIPAL);
        g2.drawString(String.format("%02d", numeroMesa), x, 58);

        // El subtítulo llega como "Estado · $total": el total arriba y el estado abajo, para que no se corte.
        String[] partes = subtitulo.split(" · ", 2);
        if (partes.length == 2) {
            g2.setFont(EstiloUI.FUENTE_TEXTO.deriveFont(Font.BOLD, 15f));
            g2.setColor(EstiloUI.TEXTO_PRINCIPAL);
            g2.drawString(partes[1], x, h - 38);
        }
        g2.setFont(EstiloUI.FUENTE_TEXTO.deriveFont(Font.BOLD, 12f));
        g2.setColor(libre ? EstiloUI.TEXTO_SECUNDARIO : colorEstado.brighter());
        g2.drawString(partes[0], x, h - 18);

        g2.dispose();
    }
}
