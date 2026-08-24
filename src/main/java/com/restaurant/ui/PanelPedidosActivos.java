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
    private final Runnable onCambioExterno;
    private final DefaultTableModel modelo;
    private final JTable tabla;

    /**
     * @param onCambioExterno se invoca después de avanzar/cancelar un pedido
     *                        desde esta pestaña, para que otras vistas que
     *                        muestran el mismo dato (p. ej. el mapa de mesas)
     *                        se refresquen también.
     */
    public PanelPedidosActivos(GestorPedidos gestor, Runnable onCambioExterno) {
        this.gestor = gestor;
        this.onCambioExterno = onCambioExterno;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(EstiloUI.FONDO);

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
        JButton btnEditar = new JButton("Editar ítems...");
        JButton btnAvanzar = new JButton("Avanzar estado ->");
        JButton btnCancelar = new JButton("Cancelar pedido X");
        botones.add(btnRefrescar);
        botones.add(btnEditar);
        botones.add(btnAvanzar);
        botones.add(btnCancelar);
        add(botones, BorderLayout.SOUTH);

        btnRefrescar.addActionListener(e -> refrescar());
        btnEditar.addActionListener(e -> editarSeleccionado());
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

    /**
     * Abre un diálogo modal para agregar/quitar platos del pedido
     * seleccionado — la misma operación que ofrece el mapa de mesas,
     * disponible también desde la vista de lista general.
     */
    private void editarSeleccionado() {
        Pedido pedido = pedidoSeleccionado();
        if (pedido == null) return;
        if (esEstadoTerminal(pedido)) {
            JOptionPane.showMessageDialog(this,
                    "El pedido #" + pedido.getId() + " ya está en estado terminal ('"
                            + pedido.getEstadoNombre() + "'); no se puede editar.");
            return;
        }

        JDialog dialogo = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Editar pedido #" + pedido.getId() + " (mesa " + pedido.getMesa() + ")", true);
        dialogo.setSize(480, 420);
        dialogo.setLocationRelativeTo(this);

        DefaultTableModel modeloItems = new DefaultTableModel(new String[]{"Plato", "Cantidad", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        Runnable refrescarDialogo = () -> {
            modeloItems.setRowCount(0);
            for (var item : pedido.getItems()) {
                modeloItems.addRow(new Object[]{
                        item.getPlato().getNombre(), item.getCantidad(), String.format("$%,.0f", item.subtotal())
                });
            }
        };
        refrescarDialogo.run();
        JTable tablaItems = new JTable(modeloItems);

        JComboBox<com.restaurant.modelo.Plato> comboPlato =
                new JComboBox<>(MenuRestaurante.PLATOS.toArray(new com.restaurant.modelo.Plato[0]));
        JSpinner spinnerCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        JButton btnAgregar = new JButton("Agregar +");
        JButton btnQuitar = new JButton("Quitar línea seleccionada");

        btnAgregar.addActionListener(e -> {
            var plato = (com.restaurant.modelo.Plato) comboPlato.getSelectedItem();
            int cantidad = (int) spinnerCantidad.getValue();
            pedido.agregarItem(new com.restaurant.modelo.ItemPedido(plato, cantidad));
            refrescarDialogo.run();
            refrescar();
            if (onCambioExterno != null) onCambioExterno.run();
        });
        btnQuitar.addActionListener(e -> {
            int fila = tablaItems.getSelectedRow();
            if (fila == -1) {
                JOptionPane.showMessageDialog(dialogo, "Selecciona una línea para quitarla.");
                return;
            }
            if (pedido.getItems().size() <= 1) {
                JOptionPane.showMessageDialog(dialogo,
                        "El pedido debe tener al menos un plato. Si quieres vaciarlo, cancélalo.");
                return;
            }
            pedido.removerItem(pedido.getItems().get(fila));
            refrescarDialogo.run();
            refrescar();
            if (onCambioExterno != null) onCambioExterno.run();
        });

        JPanel formulario = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formulario.add(new JLabel("Plato:"));
        formulario.add(comboPlato);
        formulario.add(new JLabel("Cantidad:"));
        formulario.add(spinnerCantidad);
        formulario.add(btnAgregar);

        dialogo.setLayout(new BorderLayout(8, 8));
        dialogo.add(formulario, BorderLayout.NORTH);
        dialogo.add(new JScrollPane(tablaItems), BorderLayout.CENTER);
        dialogo.add(btnQuitar, BorderLayout.SOUTH);
        dialogo.setVisible(true);
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
        if (onCambioExterno != null) onCambioExterno.run();
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
            if (onCambioExterno != null) onCambioExterno.run();
        }
    }
}
