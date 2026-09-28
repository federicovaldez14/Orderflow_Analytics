package com.restaurant.infraestructura.ui;

import com.restaurant.aplicacion.casodeuso.ServicioAnalitica;
import com.restaurant.aplicacion.casodeuso.ServicioAnalitica.PanelAnalitico;
import com.restaurant.dominio.reportes.Indicadores;
import com.restaurant.dominio.reportes.Periodo;
import com.restaurant.dominio.reportes.Reporte;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pestaña "Analítica" (Reto 3): indicadores clave arriba y gráficas abajo.
 *
 * Adaptador de entrada: pide a ServicioAnalitica los datos ya calculados
 * (estrategias de reporte del dominio) y solo se ocupa de dibujarlos. Cada
 * tarjeta tiene un botón "Tabla" que muestra los mismos números en una
 * tabla, para quien prefiera leer valores exactos.
 */
public class PanelAnalitica extends JPanel {

    private final ServicioAnalitica analitica;
    private final JComboBox<Periodo> comboPeriodo = new JComboBox<>(Periodo.values());
    private final JLabel lblConsiderados = new JLabel(" ");

    private final JLabel kpiVentas = valorKpi(true);
    private final JLabel kpiEntregados = valorKpi(false);
    private final JLabel kpiTicket = valorKpi(false);
    private final JLabel kpiTiempo = valorKpi(false);
    private final JLabel kpiCancelacion = valorKpi(false);
    private final JLabel kpiEnCurso = valorKpi(false);

    /** id del reporte -> tarjeta que lo muestra. */
    private final Map<String, TarjetaGrafico> tarjetas = new LinkedHashMap<>();

    public PanelAnalitica(ServicioAnalitica analitica) {
        this.analitica = analitica;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(EstiloUI.FONDO);

        // Fila de filtros (una sola, encima de todo)
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(EstiloUI.titulo("Analítica"));
        filtros.add(Box.createHorizontalStrut(16));
        filtros.add(new JLabel("Periodo:"));
        comboPeriodo.setSelectedItem(Periodo.TODO);
        comboPeriodo.addActionListener(e -> refrescar());
        filtros.add(comboPeriodo);
        JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> refrescar());
        filtros.add(btnActualizar);
        lblConsiderados.setForeground(GraficoBarras.TEXTO_SECUNDARIO);
        filtros.add(lblConsiderados);

        // Indicadores clave
        // La tarjeta de ventas (la cifra principal) es más ancha que las demás.
        JPanel kpis = new JPanel(new GridBagLayout());
        kpis.setOpaque(false);
        JPanel[] tiles = {tarjetaKpi("Ventas (entregados)", kpiVentas),
                tarjetaKpi("Pedidos entregados", kpiEntregados), tarjetaKpi("Ticket promedio", kpiTicket),
                tarjetaKpi("Tiempo de atención", kpiTiempo), tarjetaKpi("Tasa de cancelación", kpiCancelacion),
                tarjetaKpi("Pedidos en curso", kpiEnCurso)};
        for (int i = 0; i < tiles.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = i == 0 ? 1.8 : 1;
            c.insets = new Insets(0, i == 0 ? 0 : 10, 0, 0);
            kpis.add(tiles[i], c);
        }

        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setOpaque(false);
        norte.add(filtros, BorderLayout.NORTH);
        norte.add(kpis, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        // Gráficas
        JPanel grilla = new JPanel(new GridLayout(3, 2, 12, 12));
        grilla.setOpaque(false);
        agregar(grilla, "ventas-por-hora", "Ventas por hora del día (pedidos entregados)", GraficoBarras.Orientacion.VERTICAL);
        agregar(grilla, "platos-mas-pedidos", "Platos más pedidos (unidades)", GraficoBarras.Orientacion.HORIZONTAL);
        agregar(grilla, "ingresos-por-categoria", "Ingresos por categoría", GraficoBarras.Orientacion.HORIZONTAL);
        agregar(grilla, "tiempo-promedio", "Tiempo promedio por etapa (min)", GraficoBarras.Orientacion.HORIZONTAL);
        agregar(grilla, "pedidos-por-estado", "Pedidos por estado", GraficoBarras.Orientacion.HORIZONTAL);
        agregar(grilla, "platos-menos-pedidos", "Platos menos pedidos (unidades)", GraficoBarras.Orientacion.HORIZONTAL);
        JScrollPane scroll = new JScrollPane(grilla);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(EstiloUI.FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        refrescar();
    }

    public void refrescar() {
        Periodo periodo = (Periodo) comboPeriodo.getSelectedItem();
        PanelAnalitico panel = analitica.panel(periodo);
        Indicadores k = panel.indicadores();
        kpiVentas.setText(EstiloUI.pesos(k.ventas()));
        kpiEntregados.setText(String.format("%,d", k.pedidosEntregados()));
        kpiTicket.setText(EstiloUI.pesos(k.ticketPromedio()));
        kpiTiempo.setText(String.format("%.1f min", k.tiempoPromedioMin()));
        kpiCancelacion.setText(String.format("%.1f %%", k.tasaCancelacion() * 100));
        kpiEnCurso.setText(String.format("%,d", k.pedidosEnCurso()));
        lblConsiderados.setText(panel.pedidosConsiderados() + " pedidos en el periodo");
        for (Reporte r : panel.reportes()) {
            TarjetaGrafico t = tarjetas.get(r.id());
            if (t != null) {
                t.mostrar(r);
            }
        }
    }

    private void agregar(JPanel grilla, String id, String titulo, GraficoBarras.Orientacion o) {
        TarjetaGrafico t = new TarjetaGrafico(titulo, o);
        tarjetas.put(id, t);
        grilla.add(t);
    }

    private static JLabel valorKpi(boolean heroe) {
        JLabel l = new JLabel("-");
        l.setFont(EstiloUI.FUENTE_TITULO.deriveFont(heroe ? 30f : 22f));
        l.setForeground(GraficoBarras.TEXTO);
        return l;
    }

    private static JPanel tarjetaKpi(String etiqueta, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(EstiloUI.TARJETA);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GraficoBarras.REJILLA),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel l = new JLabel(etiqueta);
        l.setForeground(GraficoBarras.TEXTO_SECUNDARIO);
        p.add(l, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    /** Tarjeta con título, gráfico y la vista alternativa en tabla. */
    private static final class TarjetaGrafico extends JPanel {

        private final CardLayout cartas = new CardLayout();
        private final JPanel cuerpo = new JPanel(cartas);
        private final GraficoBarras grafico;
        private final DefaultTableModel modelo = new DefaultTableModel(new String[]{"", "Valor"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        TarjetaGrafico(String titulo, GraficoBarras.Orientacion o) {
            super(new BorderLayout(0, 6));
            setBackground(EstiloUI.TARJETA);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GraficoBarras.REJILLA),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            setPreferredSize(new Dimension(480, 270));

            JPanel cabecera = new JPanel(new BorderLayout());
            cabecera.setOpaque(false);
            JLabel lbl = EstiloUI.subtitulo(titulo);
            lbl.setForeground(GraficoBarras.TEXTO);
            cabecera.add(lbl, BorderLayout.WEST);
            JToggleButton btnTabla = new JToggleButton("Tabla");
            btnTabla.setToolTipText("Ver los mismos datos como tabla");
            btnTabla.addActionListener(e -> cartas.show(cuerpo, btnTabla.isSelected() ? "tabla" : "grafico"));
            cabecera.add(btnTabla, BorderLayout.EAST);
            add(cabecera, BorderLayout.NORTH);

            grafico = new GraficoBarras(o);
            cuerpo.add(grafico, "grafico");
            cuerpo.add(new JScrollPane(new JTable(modelo)), "tabla");
            add(cuerpo, BorderLayout.CENTER);
        }

        void mostrar(Reporte r) {
            grafico.mostrar(r);
            modelo.setRowCount(0);
            for (Reporte.Dato d : r.datos()) {
                modelo.addRow(new Object[]{d.etiqueta(), GraficoBarras.completo(d.valor(), r.unidad())});
            }
        }
    }
}
