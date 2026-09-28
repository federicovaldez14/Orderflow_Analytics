package com.restaurant.infraestructura.ui;

import com.restaurant.aplicacion.casodeuso.ServicioInventario;
import com.restaurant.dominio.inventario.Ingrediente;
import com.restaurant.dominio.inventario.MovimientoInventario;
import com.restaurant.infraestructura.notificacion.AlertasInventarioEnMemoria;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Pestaña "Inventario" (Reto 1). Adaptador de entrada: muestra el stock, lo
 * que alcanza a prepararse y el historial de movimientos (trazabilidad), y
 * permite reponer o ajustar por conteo físico a través de ServicioInventario.
 */
public class PanelInventario extends JPanel {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
    private static final Color ROJO_SUAVE = new Color(0x3A, 0x1F, 0x1E);

    private final ServicioInventario inventario;

    private final DefaultTableModel modeloIngredientes = soloLectura("Código", "Ingrediente", "Stock", "Unidad", "Mínimo", "Estado");
    private final JTable tablaIngredientes = new JTable(modeloIngredientes);
    private final DefaultTableModel modeloPorciones = soloLectura("Plato", "Porciones posibles");
    private final DefaultTableModel modeloMovimientos = soloLectura("Fecha", "Ingrediente", "Tipo", "Cantidad",
            "Stock después", "Pedido", "Detalle");
    private final JLabel lblAlerta = new JLabel(" ");
    private final JLabel lblMovimientos = EstiloUI.subtitulo("Movimientos (todos)");

    public PanelInventario(ServicioInventario inventario, AlertasInventarioEnMemoria alertas) {
        this.inventario = inventario;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(EstiloUI.FONDO);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(EstiloUI.titulo("Inventario de ingredientes"), BorderLayout.WEST);
        lblAlerta.setForeground(EstiloUI.PELIGRO);
        lblAlerta.setFont(EstiloUI.FUENTE_SUBTITULO);
        norte.add(lblAlerta, BorderLayout.EAST);
        add(norte, BorderLayout.NORTH);

        tablaIngredientes.setRowHeight(22);
        tablaIngredientes.setDefaultRenderer(Object.class, new RenderizadorEstado());
        tablaIngredientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refrescarMovimientos();
            }
        });

        JSplitPane arriba = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(tablaIngredientes), bloque("Lo que alcanza a prepararse", new JTable(modeloPorciones)));
        arriba.setResizeWeight(0.68);

        JPanel abajo = new JPanel(new BorderLayout(4, 4));
        abajo.setOpaque(false);
        abajo.add(lblMovimientos, BorderLayout.NORTH);
        abajo.add(new JScrollPane(new JTable(modeloMovimientos)), BorderLayout.CENTER);

        JSplitPane centro = new JSplitPane(JSplitPane.VERTICAL_SPLIT, arriba, abajo);
        centro.setResizeWeight(0.55);
        add(centro, BorderLayout.CENTER);

        JButton btnReponer = new JButton("Reponer...");
        JButton btnAjustar = new JButton("Ajustar por conteo físico...");
        JButton btnTodos = new JButton("Ver todos los movimientos");
        btnReponer.addActionListener(e -> reponer());
        btnAjustar.addActionListener(e -> ajustar());
        btnTodos.addActionListener(e -> {
            tablaIngredientes.clearSelection();
            refrescarMovimientos();
        });
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.setOpaque(false);
        botones.add(btnReponer);
        botones.add(btnAjustar);
        botones.add(btnTodos);
        add(botones, BorderLayout.SOUTH);

        alertas.suscribir(linea -> SwingUtilities.invokeLater(this::refrescar));
        refrescar();
    }

    public void refrescar() {
        String seleccionado = codigoSeleccionado();
        List<Ingrediente> ingredientes = inventario.ingredientes();
        modeloIngredientes.setRowCount(0);
        int bajos = 0;
        for (Ingrediente i : ingredientes) {
            boolean bajo = i.bajoMinimo();
            if (bajo) {
                bajos++;
            }
            modeloIngredientes.addRow(new Object[]{i.getCodigo(), i.getNombre(), String.format("%,d", i.getStock()),
                    i.getUnidad().getSimbolo(), String.format("%,d", i.getStockMinimo()), bajo ? "BAJO MÍNIMO" : "OK"});
        }
        lblAlerta.setText(bajos == 0 ? " " : "⚠ " + bajos + " ingrediente(s) bajo el mínimo");
        if (seleccionado != null) {
            for (int f = 0; f < modeloIngredientes.getRowCount(); f++) {
                if (seleccionado.equals(modeloIngredientes.getValueAt(f, 0))) {
                    tablaIngredientes.setRowSelectionInterval(f, f);
                }
            }
        }

        modeloPorciones.setRowCount(0);
        for (Map.Entry<String, Long> e : inventario.porcionesDisponibles().entrySet()) {
            modeloPorciones.addRow(new Object[]{e.getKey(), e.getValue()});
        }
        refrescarMovimientos();
    }

    private void refrescarMovimientos() {
        String codigo = codigoSeleccionado();
        lblMovimientos.setText(codigo == null ? "Movimientos (todos, últimos 200)" : "Movimientos de " + codigo);
        modeloMovimientos.setRowCount(0);
        for (MovimientoInventario m : inventario.movimientos(codigo, 200)) {
            modeloMovimientos.addRow(new Object[]{m.fecha().format(FECHA), m.ingrediente(), m.tipo(),
                    String.format("%,d", m.cantidad()), String.format("%,d", m.stockResultante()),
                    m.pedidoId() == null ? "" : "#" + m.pedidoId(), m.descripcion()});
        }
    }

    private String codigoSeleccionado() {
        int fila = tablaIngredientes.getSelectedRow();
        return fila == -1 ? null : (String) modeloIngredientes.getValueAt(fila, 0);
    }

    private void reponer() {
        String codigo = exigirSeleccion();
        if (codigo == null) return;
        String texto = JOptionPane.showInputDialog(this, "Cantidad a reponer de " + codigo + " (en su unidad base):");
        if (texto == null) return;
        String nota = JOptionPane.showInputDialog(this, "Nota (proveedor, factura...):", "Compra");
        EstiloUI.intentar(this, () -> inventario.reponer(codigo, Long.parseLong(texto.trim()), nota));
        refrescar();
    }

    private void ajustar() {
        String codigo = exigirSeleccion();
        if (codigo == null) return;
        String texto = JOptionPane.showInputDialog(this, "¿Cuánto hay realmente de " + codigo + " según el conteo?");
        if (texto == null) return;
        EstiloUI.intentar(this, () -> inventario.ajustarConteo(codigo, Long.parseLong(texto.trim()), "Conteo físico"));
        refrescar();
    }

    private String exigirSeleccion() {
        String codigo = codigoSeleccionado();
        if (codigo == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un ingrediente de la tabla primero.");
        }
        return codigo;
    }

    private static DefaultTableModel soloLectura(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private static JComponent bloque(String titulo, JTable tabla) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(titulo));
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return p;
    }

    /** Pinta de rojo suave las filas bajo el mínimo. */
    private static final class RenderizadorEstado extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
            boolean bajo = "BAJO MÍNIMO".equals(t.getModel().getValueAt(fila, 5));
            if (!sel) {
                c.setBackground(bajo ? ROJO_SUAVE : t.getBackground());
            }
            return c;
        }
    }
}
