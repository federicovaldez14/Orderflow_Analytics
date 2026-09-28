package com.restaurant.infraestructura.ui;

import com.restaurant.aplicacion.casodeuso.GestorPedidos;
import com.restaurant.aplicacion.casodeuso.LineaSolicitada;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.modelo.Plato;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Lista de todos los pedidos, con acciones de editar, avanzar y cancelar.
 * Adaptador de entrada: cada acción llama al caso de uso por id de pedido.
 */
public class PanelPedidosActivos extends JPanel {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String[] COLUMNAS = {"ID", "Mesa", "Estado", "Total", "Items", "Creado"};

    private final GestorPedidos gestor;
    private final Runnable onCambioExterno;
    private final PanelMapaMesas.AccionesCuenta accionesCuenta;
    private final DefaultTableModel modelo;
    private final JTable tabla;

    public PanelPedidosActivos(GestorPedidos gestor, Runnable onCambioExterno,
                               PanelMapaMesas.AccionesCuenta accionesCuenta) {
        this.gestor = gestor;
        this.onCambioExterno = onCambioExterno;
        this.accionesCuenta = accionesCuenta;
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
        JButton btnDividir = new JButton("Dividir cuenta...");
        btnDividir.setVisible(accionesCuenta != null);
        btnDividir.addActionListener(e -> {
            Integer id = exigirSeleccion();
            if (id != null) {
                accionesCuenta.dividirCuenta(this, id);
            }
        });
        botones.add(btnRefrescar);
        botones.add(btnEditar);
        botones.add(btnAvanzar);
        botones.add(btnCancelar);
        botones.add(btnDividir);
        add(botones, BorderLayout.SOUTH);

        btnRefrescar.addActionListener(e -> refrescar());
        btnEditar.addActionListener(e -> editarSeleccionado());
        btnAvanzar.addActionListener(e -> avanzarSeleccionado());
        btnCancelar.addActionListener(e -> cancelarSeleccionado());

        refrescar();
    }

    public void refrescar() {
        Integer seleccionado = idSeleccionado();
        modelo.setRowCount(0);
        for (Pedido p : gestor.listar()) {
            modelo.addRow(new Object[]{
                    p.getId(), p.getMesa(), p.getEstadoNombre(), EstiloUI.pesos(p.calcularTotal()),
                    resumenItems(p), p.getHoraCreacion().format(HORA)
            });
        }
        if (seleccionado != null) {
            for (int fila = 0; fila < modelo.getRowCount(); fila++) {
                if (seleccionado.equals(modelo.getValueAt(fila, 0))) {
                    tabla.setRowSelectionInterval(fila, fila);
                }
            }
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

    private Integer idSeleccionado() {
        int fila = tabla.getSelectedRow();
        return fila == -1 ? null : (Integer) modelo.getValueAt(fila, 0);
    }

    private Integer exigirSeleccion() {
        Integer id = idSeleccionado();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un pedido de la tabla primero.");
        }
        return id;
    }

    private void cambio() {
        refrescar();
        if (onCambioExterno != null) onCambioExterno.run();
    }

    /** Diálogo modal para agregar/quitar platos del pedido seleccionado. */
    private void editarSeleccionado() {
        Integer id = exigirSeleccion();
        if (id == null) return;
        Pedido pedido = gestor.obtener(id);
        if (!pedido.estaEditable()) {
            JOptionPane.showMessageDialog(this, "El pedido #" + id + " ya está '"
                    + pedido.getEstadoNombre() + "'; no se puede editar.");
            return;
        }

        JDialog dialogo = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Editar pedido #" + id + " (mesa " + pedido.getMesa() + ")", true);
        dialogo.setSize(520, 420);
        dialogo.setLocationRelativeTo(this);

        DefaultTableModel modeloItems = new DefaultTableModel(new String[]{"Plato", "Cantidad", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        Runnable refrescarDialogo = () -> {
            modeloItems.setRowCount(0);
            for (ItemPedido item : gestor.obtener(id).getItems()) {
                modeloItems.addRow(new Object[]{
                        item.getPlato().getNombre(), item.getCantidad(), EstiloUI.pesos(item.subtotal())
                });
            }
        };
        refrescarDialogo.run();
        JTable tablaItems = new JTable(modeloItems);

        JComboBox<Plato> comboPlato = new JComboBox<>(gestor.menu().toArray(new Plato[0]));
        JSpinner spinnerCantidad = new JSpinner(new SpinnerNumberModel(1, 1, ItemPedido.CANTIDAD_MAXIMA, 1));
        JButton btnAgregar = new JButton("Agregar +");
        JButton btnQuitar = new JButton("Quitar línea seleccionada");

        btnAgregar.addActionListener(e -> {
            Plato plato = (Plato) comboPlato.getSelectedItem();
            int cantidad = (int) spinnerCantidad.getValue();
            EstiloUI.intentar(dialogo,
                    () -> gestor.agregarItem(id, new LineaSolicitada(plato.getNombre(), cantidad)));
            refrescarDialogo.run();
            cambio();
        });
        btnQuitar.addActionListener(e -> {
            int fila = tablaItems.getSelectedRow();
            if (fila == -1) {
                JOptionPane.showMessageDialog(dialogo, "Selecciona una línea para quitarla.");
                return;
            }
            EstiloUI.intentar(dialogo, () -> gestor.quitarLinea(id, fila));
            refrescarDialogo.run();
            cambio();
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
        Integer id = exigirSeleccion();
        if (id == null) return;
        EstiloUI.intentar(this, () -> gestor.avanzarEstado(id));
        cambio();
    }

    private void cancelarSeleccionado() {
        Integer id = exigirSeleccion();
        if (id == null) return;
        int confirmacion = JOptionPane.showConfirmDialog(this, "¿Cancelar el pedido #" + id + "?",
                "Confirmar cancelación", JOptionPane.YES_NO_OPTION);
        if (confirmacion == JOptionPane.YES_OPTION) {
            EstiloUI.intentar(this, () -> gestor.cancelar(id));
            cambio();
        }
    }
}
