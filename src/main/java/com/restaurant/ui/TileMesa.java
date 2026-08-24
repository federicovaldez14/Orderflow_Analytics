package com.restaurant.ui;

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

        int w = getWidth();
        int h = getHeight();

        g2.setColor(colorEstado);
        g2.fillRoundRect(4, 4, w - 8, h - 8, 18, 18);

        if (seleccionada) {
            g2.setColor(EstiloUI.TEXTO_PRINCIPAL);
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(4, 4, w - 9, h - 9, 18, 18);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(EstiloUI.FUENTE_MESA);
        String textoMesa = "Mesa " + numeroMesa;
        FontMetrics fmTitulo = g2.getFontMetrics();
        g2.drawString(textoMesa, (w - fmTitulo.stringWidth(textoMesa)) / 2, h / 2 - 4);

        g2.setFont(EstiloUI.FUENTE_TEXTO);
        FontMetrics fmSub = g2.getFontMetrics();
        g2.drawString(subtitulo, (w - fmSub.stringWidth(subtitulo)) / 2, h / 2 + 16);

        g2.dispose();
    }
}
