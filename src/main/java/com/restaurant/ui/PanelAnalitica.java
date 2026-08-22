package com.restaurant.ui;

import com.restaurant.reportes.EstrategiaReporte;
import com.restaurant.reportes.ReportePlatosMasPedidos;
import com.restaurant.reportes.ReportePlatosMenosPedidos;
import com.restaurant.reportes.ReporteService;
import com.restaurant.reportes.ReporteTiempoPromedio;
import com.restaurant.servicio.GestorPedidos;

import javax.swing.*;
import java.awt.*;

/**
 * Panel de analítica (patrón Strategy): cada botón dispara una
 * estrategia de reporte distinta sobre los pedidos actuales del gestor.
 * Los datos se recalculan en el momento del clic, así que reflejan
 * exactamente los pedidos que hayas creado/avanzado hasta ese instante.
 */
public class PanelAnalitica extends JPanel {

    private final GestorPedidos gestor;
    private final ReporteService reporteService = new ReporteService();
    private final JTextArea areaResultado = new JTextArea();

    public PanelAnalitica(GestorPedidos gestor) {
        this.gestor = gestor;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnMas = new JButton("Platos más pedidos");
        JButton btnMenos = new JButton("Platos menos pedidos");
        JButton btnTiempo = new JButton("Tiempo promedio de atención");
        botones.add(btnMas);
        botones.add(btnMenos);
        botones.add(btnTiempo);
        add(botones, BorderLayout.NORTH);

        areaResultado.setEditable(false);
        areaResultado.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        add(new JScrollPane(areaResultado), BorderLayout.CENTER);

        btnMas.addActionListener(e -> mostrar(new ReportePlatosMasPedidos()));
        btnMenos.addActionListener(e -> mostrar(new ReportePlatosMenosPedidos()));
        btnTiempo.addActionListener(e -> mostrar(new ReporteTiempoPromedio()));
    }

    private void mostrar(EstrategiaReporte estrategia) {
        areaResultado.setText(reporteService.generarReporte(estrategia, gestor.getPedidos()));
    }
}
