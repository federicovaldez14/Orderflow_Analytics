package com.restaurant.ui;

import com.restaurant.estado.EstadoCancelado;
import com.restaurant.modelo.ItemPedido;
import com.restaurant.modelo.Pedido;
import com.restaurant.servicio.GestorPedidos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Panel de lectura y actualización de pedidos (la "R", la "U" y, con la
 * cancelación, una forma de "D" del CRUD): lista todos los pedidos
 * existentes y permite avanzar su estado o cancelarlos.
 */
public class PanelPedidosActivos extends JPanel {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String[] COLUMNAS = {"ID", "Mesa", "Estado", "Total", "Items", "Creado"};

    private final GestorPedidos gestor;
    private final DefaultTableModel modelo;
    private final JTable tabla;

    public PanelPedidosActivos(GestorPedidos gestor) {
        this.gestor = gestor;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        modelo = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(24);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefrescar = new JButton("Refrescar");
        JButton btnAvanzar = new JButton("Avanzar estado ->");
        JButton btnCancelar = new JButton("Cancelar pedido X");
        botones.add(btnRefrescar);
        botones.add(btnAvanzar);
        botones.add(btnCancelar);
        add(botones, BorderLayout.SOUTH);

        btnRefrescar.addActionListener(e -> refrescar());
        btnAvanzar.addActionListener(e -> avanzarSeleccionado());
        btnCancelar.addActionListener(e -> cancelarSeleccionado());

        refrescar();
    }

    public void refrescar() {
        modelo.setRowCount(0);
        for (Pedido p : gestor.getPedidos()) {
            modelo.addRow(new Object[]{
                    p.getId(),
                    p.getMesa(),
                    p.getEstadoNombre(),
                    String.format("$%,.0f", p.calcularTotal()),
                    resumenItems(p),
                    p.getHoraCreacion().format(HORA)
            });
        }
    }

    private String resumenItems(Pedido p) {
        StringBuilder sb = new StringBuilder();
        for (ItemPedido item : p.getItems()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(item.getCantidad()).append("x ").append(item.getPlato().getNombre());
        }
        return sb.toString();
    }

    private Pedido pedidoSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un pedido de la tabla primero.");
            return null;
        }
        int id = (int) modelo.getValueAt(fila, 0);
        return gestor.getPedidos().stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    private boolean esEstadoTerminal(Pedido pedido) {
        return pedido.getEstadoNombre().equals("Entregado") || pedido.getEstadoNombre().equals("Cancelado");
    }

    private void avanzarSeleccionado() {
        Pedido pedido = pedidoSeleccionado();
        if (pedido == null) return;
        if (esEstadoTerminal(pedido)) {
            JOptionPane.showMessageDialog(this,
                    "El pedido #" + pedido.getId() + " ya está en estado terminal ('"
                            + pedido.getEstadoNombre() + "'); no puede avanzar más.");
            return;
        }
        gestor.avanzarEstado(pedido);
        refrescar();
    }

    private void cancelarSeleccionado() {
        Pedido pedido = pedidoSeleccionado();
        if (pedido == null) return;
        if (esEstadoTerminal(pedido)) {
            JOptionPane.showMessageDialog(this,
                    "El pedido #" + pedido.getId() + " ya está en estado terminal ('"
                            + pedido.getEstadoNombre() + "'); no se puede cancelar.");
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Cancelar el pedido #" + pedido.getId() + " de la mesa " + pedido.getMesa() + "?",
                "Confirmar cancelación", JOptionPane.YES_NO_OPTION);
        if (confirmacion == JOptionPane.YES_OPTION) {
            pedido.cambiarEstado(new EstadoCancelado());
            refrescar();
        }
    }
}
