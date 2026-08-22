package com.restaurant.ui;

import com.restaurant.modelo.ItemPedido;
import com.restaurant.modelo.Pedido;
import com.restaurant.modelo.Plato;
import com.restaurant.servicio.GestorPedidos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de creación de pedidos (la "C" del CRUD): simula lo que haría un
 * mesero tomando la comanda — elige mesa, agrega platos del menú ya
 * existente con su cantidad, arma un "carrito" y confirma el pedido.
 */
public class PanelNuevoPedido extends JPanel {

    private final GestorPedidos gestor;
    private final Runnable alCrearPedido;

    private final List<ItemPedido> carrito = new ArrayList<>();
    private final DefaultTableModel modeloCarrito;
    private final JLabel lblTotal = new JLabel("Total: $0");
    private final JComboBox<Integer> comboMesa;
    private final JComboBox<Plato> comboPlato;
    private final JSpinner spinnerCantidad;

    public PanelNuevoPedido(GestorPedidos gestor, Runnable alCrearPedido) {
        this.gestor = gestor;
        this.alCrearPedido = alCrearPedido;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formulario = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboMesa = new JComboBox<>(MenuRestaurante.MESAS.toArray(new Integer[0]));
        comboPlato = new JComboBox<>(MenuRestaurante.PLATOS.toArray(new Plato[0]));
        spinnerCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        JButton btnAgregar = new JButton("Agregar al pedido +");

        formulario.add(new JLabel("Mesa:"));
        formulario.add(comboMesa);
        formulario.add(new JLabel("Plato:"));
        formulario.add(comboPlato);
        formulario.add(new JLabel("Cantidad:"));
        formulario.add(spinnerCantidad);
        formulario.add(btnAgregar);
        add(formulario, BorderLayout.NORTH);

        modeloCarrito = new DefaultTableModel(new String[]{"Plato", "Cantidad", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable tablaCarrito = new JTable(modeloCarrito);
        add(new JScrollPane(tablaCarrito), BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout());
        JPanel botonesAbajo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnQuitar = new JButton("Quitar línea seleccionada");
        JButton btnCrear = new JButton("Crear pedido");
        botonesAbajo.add(btnQuitar);
        botonesAbajo.add(btnCrear);
        abajo.add(lblTotal, BorderLayout.WEST);
        abajo.add(botonesAbajo, BorderLayout.EAST);
        add(abajo, BorderLayout.SOUTH);

        btnAgregar.addActionListener(e -> agregarAlCarrito());
        btnQuitar.addActionListener(e -> quitarSeleccionado(tablaCarrito));
        btnCrear.addActionListener(e -> crearPedido());
    }

    private void agregarAlCarrito() {
        Plato plato = (Plato) comboPlato.getSelectedItem();
        int cantidad = (int) spinnerCantidad.getValue();
        if (plato == null) return;
        carrito.add(new ItemPedido(plato, cantidad));
        refrescarCarrito();
    }

    private void quitarSeleccionado(JTable tablaCarrito) {
        int fila = tablaCarrito.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una línea del pedido para quitarla.");
            return;
        }
        carrito.remove(fila);
        refrescarCarrito();
    }

    private void refrescarCarrito() {
        modeloCarrito.setRowCount(0);
        double total = 0;
        for (ItemPedido item : carrito) {
            modeloCarrito.addRow(new Object[]{
                    item.getPlato().getNombre(), item.getCantidad(), String.format("$%,.0f", item.subtotal())
            });
            total += item.subtotal();
        }
        lblTotal.setText(String.format("Total: $%,.0f", total));
    }

    private void crearPedido() {
        if (carrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agrega al menos un plato antes de crear el pedido.");
            return;
        }
        int mesa = (Integer) comboMesa.getSelectedItem();
        Pedido pedido = gestor.crearPedido(mesa);
        for (ItemPedido item : carrito) {
            pedido.agregarItem(item);
        }

        JOptionPane.showMessageDialog(this,
                "Pedido #" + pedido.getId() + " creado para la mesa " + mesa + ".");

        carrito.clear();
        refrescarCarrito();
        alCrearPedido.run(); // avisa a la ventana principal para refrescar la pestaña de pedidos activos
    }
}
