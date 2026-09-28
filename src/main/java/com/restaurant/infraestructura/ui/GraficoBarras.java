package com.restaurant.infraestructura.ui;

import com.restaurant.dominio.reportes.Reporte;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Gráfico de barras dibujado con Java2D (sin librerías externas), igual que
 * TileMesa dibuja las mesas. Una sola serie por gráfico, así que el título
 * de la tarjeta la nombra y no hace falta leyenda.
 *
 * Decisiones de diseño (legibilidad):
 *  - barras delgadas (máx. 22 px) que nacen de una línea base común, con el
 *    extremo del dato redondeado y la base recta;
 *  - cuadrícula y ejes tenues, el color fuerte queda solo para los datos;
 *  - el valor va en la punta de cada barra, en color de texto (no de la serie);
 *  - al pasar el mouse sobre una barra, un tooltip muestra el valor exacto.
 */
public class GraficoBarras extends JComponent {

    public enum Orientacion { HORIZONTAL, VERTICAL }

    static final Color SERIE = new Color(0x2a, 0x78, 0xd6);
    static final Color TEXTO = new Color(0x0b, 0x0b, 0x0b);
    static final Color TEXTO_SECUNDARIO = new Color(0x52, 0x51, 0x4e);
    static final Color REJILLA = new Color(0xE6, 0xE3, 0xDC);
    private static final Font FUENTE = new Font("SansSerif", Font.PLAIN, 12);
    private static final int GROSOR_MAX = 22;
    private static final int RADIO = 4;

    private final Orientacion orientacion;
    private Reporte reporte;
    private final List<Rectangle> zonas = new ArrayList<>();

    public GraficoBarras(Orientacion orientacion) {
        this.orientacion = orientacion;
        setPreferredSize(new Dimension(420, 240));
        setToolTipText("");   // activa el tooltip por barra (getToolTipText(MouseEvent))
        setOpaque(true);
        setBackground(Color.WHITE);
    }

    public void mostrar(Reporte reporte) {
        this.reporte = reporte;
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (reporte == null) {
            return null;
        }
        for (int i = 0; i < zonas.size() && i < reporte.datos().size(); i++) {
            if (zonas.get(i).contains(e.getPoint())) {
                Reporte.Dato d = reporte.datos().get(i);
                return d.etiqueta() + ": " + completo(d.valor(), reporte.unidad());
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setFont(FUENTE);
        zonas.clear();

        if (reporte == null || reporte.vacio()) {
            g2.setColor(TEXTO_SECUNDARIO);
            String msg = "Sin datos en este periodo";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
            g2.dispose();
            return;
        }
        double max = reporte.datos().stream().mapToDouble(Reporte.Dato::valor).max().orElse(0);
        double[] escala = escala(max);
        if (orientacion == Orientacion.HORIZONTAL) {
            pintarHorizontal(g2, escala[0], (int) escala[1]);
        } else {
            pintarVertical(g2, escala[0], (int) escala[1]);
        }
        g2.dispose();
    }

    private void pintarHorizontal(Graphics2D g2, double tope, int marcas) {
        FontMetrics fm = g2.getFontMetrics();
        List<Reporte.Dato> datos = reporte.datos();
        int anchoEtiquetas = 0;
        for (Reporte.Dato d : datos) {
            anchoEtiquetas = Math.max(anchoEtiquetas, fm.stringWidth(d.etiqueta()));
        }
        anchoEtiquetas = Math.min(anchoEtiquetas, Math.max(90, getWidth() / 3));
        int x0 = 12 + anchoEtiquetas + 10;
        int anchoValor = fm.stringWidth(corto(tope, reporte.unidad())) + 14;
        int x1 = getWidth() - anchoValor;
        int y0 = 8;
        int y1 = getHeight() - 22;
        if (x1 <= x0 + 20 || y1 <= y0 + 10) {
            return;
        }

        // Rejilla vertical tenue con marcas redondas
        for (int k = 0; k <= marcas; k++) {
            int x = x0 + (int) Math.round((x1 - x0) * k / (double) marcas);
            g2.setColor(REJILLA);
            g2.drawLine(x, y0, x, y1);
            g2.setColor(TEXTO_SECUNDARIO);
            String t = eje(tope * k / marcas, reporte.unidad());
            g2.drawString(t, x - fm.stringWidth(t) / 2, y1 + fm.getAscent() + 4);
        }

        double banda = (y1 - y0) / (double) datos.size();
        int grosor = (int) Math.max(4, Math.min(GROSOR_MAX, banda * 0.62));
        for (int i = 0; i < datos.size(); i++) {
            Reporte.Dato d = datos.get(i);
            int yc = y0 + (int) Math.round(banda * i + banda / 2);
            int largo = (int) Math.round((x1 - x0) * (d.valor() / tope));
            g2.setColor(SERIE);
            if (largo > 0) {
                g2.fill(barraHorizontal(x0, yc - grosor / 2, largo, grosor));
            }
            g2.setColor(TEXTO);
            String etiqueta = recortar(d.etiqueta(), anchoEtiquetas, fm);
            g2.drawString(etiqueta, x0 - 10 - fm.stringWidth(etiqueta), yc + fm.getAscent() / 2 - 1);
            g2.drawString(corto(d.valor(), reporte.unidad()), x0 + largo + 6, yc + fm.getAscent() / 2 - 1);
            zonas.add(new Rectangle(0, (int) (y0 + banda * i), getWidth(), (int) Math.ceil(banda)));
        }
        g2.setColor(TEXTO_SECUNDARIO);
        g2.drawLine(x0, y0, x0, y1);   // línea base
    }

    private void pintarVertical(Graphics2D g2, double tope, int marcas) {
        FontMetrics fm = g2.getFontMetrics();
        List<Reporte.Dato> datos = reporte.datos();
        int anchoEje = fm.stringWidth(eje(tope, reporte.unidad())) + 12;
        int x0 = anchoEje;
        int x1 = getWidth() - 10;
        int y0 = 18;
        int y1 = getHeight() - 24;
        if (x1 <= x0 + 20 || y1 <= y0 + 10) {
            return;
        }
        for (int k = 0; k <= marcas; k++) {
            int y = y1 - (int) Math.round((y1 - y0) * k / (double) marcas);
            g2.setColor(REJILLA);
            g2.drawLine(x0, y, x1, y);
            g2.setColor(TEXTO_SECUNDARIO);
            String t = eje(tope * k / marcas, reporte.unidad());
            g2.drawString(t, x0 - 6 - fm.stringWidth(t), y + fm.getAscent() / 2 - 1);
        }
        double banda = (x1 - x0) / (double) datos.size();
        int grosor = (int) Math.max(4, Math.min(GROSOR_MAX, banda * 0.62));
        // Si no caben todas las etiquetas, se muestra una sí y otra no.
        int maxEtiqueta = datos.stream().mapToInt(d -> fm.stringWidth(d.etiqueta())).max().orElse(0);
        int salto = Math.max(1, (int) Math.ceil((maxEtiqueta + 6) / banda));
        // Valor en la punta solo si hay espacio para todos; si no, queda en el tooltip.
        boolean conValores = fm.stringWidth(corto(tope, reporte.unidad())) + 4 < banda;
        for (int i = 0; i < datos.size(); i++) {
            Reporte.Dato d = datos.get(i);
            int xc = x0 + (int) Math.round(banda * i + banda / 2);
            int alto = (int) Math.round((y1 - y0) * (d.valor() / tope));
            g2.setColor(SERIE);
            if (alto > 0) {
                g2.fill(barraVertical(xc - grosor / 2, y1 - alto, grosor, alto));
            }
            g2.setColor(TEXTO);
            if (conValores && d.valor() > 0) {
                String v = corto(d.valor(), reporte.unidad());
                g2.drawString(v, xc - fm.stringWidth(v) / 2, y1 - alto - 4);
            }
            if (i % salto == 0) {
                g2.setColor(TEXTO_SECUNDARIO);
                g2.drawString(d.etiqueta(), xc - fm.stringWidth(d.etiqueta()) / 2, y1 + fm.getAscent() + 4);
            }
            zonas.add(new Rectangle((int) (x0 + banda * i), 0, (int) Math.ceil(banda), getHeight()));
        }
        g2.setColor(TEXTO_SECUNDARIO);
        g2.drawLine(x0, y1, x1, y1);   // línea base
    }

    /** Barra con la base (izquierda) recta y el extremo del dato redondeado. */
    private static Shape barraHorizontal(int x, int y, int largo, int grosor) {
        int r = Math.min(RADIO, Math.min(largo, grosor / 2));
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x + largo - r, y);
        p.quadTo(x + largo, y, x + largo, y + r);
        p.lineTo(x + largo, y + grosor - r);
        p.quadTo(x + largo, y + grosor, x + largo - r, y + grosor);
        p.lineTo(x, y + grosor);
        p.closePath();
        return p;
    }

    /** Columna con la base (abajo) recta y la punta redondeada. */
    private static Shape barraVertical(int x, int y, int grosor, int alto) {
        int r = Math.min(RADIO, Math.min(alto, grosor / 2));
        Path2D p = new Path2D.Double();
        p.moveTo(x, y + alto);
        p.lineTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.lineTo(x + grosor - r, y);
        p.quadTo(x + grosor, y, x + grosor, y + r);
        p.lineTo(x + grosor, y + alto);
        p.closePath();
        return p;
    }

    /**
     * Escala del eje con números "redondos": el paso más pequeño de la serie
     * 1-2-2,5-5 x 10^n que cubre el máximo con 5 marcas o menos.
     * Devuelve {tope, numeroDeMarcas}.
     */
    static double[] escala(double max) {
        if (max <= 0) {
            return new double[]{1, 1};
        }
        double exp = Math.pow(10, Math.floor(Math.log10(max / 5)));
        double[] factores = {1, 2, 2.5, 5, 10, 20};
        for (double f : factores) {
            double paso = f * exp;
            double marcas = Math.ceil(max / paso - 1e-9);
            if (marcas <= 5) {
                return new double[]{marcas * paso, marcas};
            }
        }
        return new double[]{max, 1};
    }

    /** Valor compacto para la punta de la barra y los ejes: $1,2 M · $85 k · 12,5 min. */
    static String corto(double v, String unidad) {
        if ("$".equals(unidad)) {
            if (v >= 1_000_000) {
                return String.format("$%.1f M", v / 1_000_000);
            }
            if (v >= 1_000) {
                return String.format("$%.0f k", v / 1_000);
            }
            return String.format("$%.0f", v);
        }
        if ("min".equals(unidad)) {
            return String.format("%.1f min", v);
        }
        return v == Math.rint(v) ? String.format("%,d", (long) v) : String.format("%,.1f", v);
    }

    /** Marca del eje: igual que corto() pero sin repetir "min" (ya está en el título). */
    static String eje(double v, String unidad) {
        if ("min".equals(unidad)) {
            return v == Math.rint(v) ? String.format("%d", (long) v) : String.format("%.1f", v);
        }
        return corto(v, unidad);
    }

    /** Valor exacto para el tooltip y la tabla. */
    static String completo(double v, String unidad) {
        if ("$".equals(unidad)) {
            return String.format("$%,d", Math.round(v));
        }
        if ("min".equals(unidad)) {
            return String.format("%.1f minutos", v);
        }
        return String.format("%,d %s", Math.round(v), unidad);
    }

    private static String recortar(String texto, int ancho, FontMetrics fm) {
        if (fm.stringWidth(texto) <= ancho) {
            return texto;
        }
        String s = texto;
        while (s.length() > 1 && fm.stringWidth(s + "…") > ancho) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }
}
