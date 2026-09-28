package com.restaurant.infraestructura.ui;

import com.restaurant.aplicacion.casodeuso.GestorPedidos;
import com.restaurant.dominio.reportes.EstrategiaReporte;
import com.restaurant.dominio.reportes.ReportePlatosMasPedidos;
import com.restaurant.dominio.reportes.ReportePlatosMenosPedidos;
import com.restaurant.dominio.reportes.ReporteService;
import com.restaurant.dominio.reportes.ReporteTiempoPromedio;

import javax.swing.*;
import java.awt.*;

/** Panel de analítica (patrón Strategy): cada botón dispara una estrategia de reporte. */
public class PanelAnalitica extends JPanel {

    private final GestorPedidos gestor;
    private final ReporteService reporteService = new ReporteService();
    private final JTextArea areaResultado = new JTextArea();

    public PanelAnalitica(GestorPedidos gestor) {
        this.gestor = gestor;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(EstiloUI.FONDO);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.setOpaque(false);
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
        areaResultado.setText(reporteService.generarReporte(estrategia, gestor.listar()));
    }
}
