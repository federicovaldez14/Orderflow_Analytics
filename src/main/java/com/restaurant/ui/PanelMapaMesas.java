package com.restaurant.ui;

import com.restaurant.estado.EstadoCancelado;
import com.restaurant.modelo.ItemPedido;
import com.restaurant.modelo.Pedido;
import com.restaurant.modelo.Plato;
import com.restaurant.servicio.GestorPedidos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista de "mapa del salón": una cuadrícula de mesas coloreada según su
 * estado. Al hacer clic en una mesa se abre, a la derecha, el
 * formulario para tomar un pedido nuevo (mesa libre) o el detalle
 * editable del pedido en curso (mesa ocupada) — agregar/quitar platos,
 * avanzar de estado o cancelar.
 *
 * Esta pantalla NO reemplaza "Pedidos activos" / "Notificaciones" /
 * "Analítica": es una forma alternativa, más visual, de llegar a las
 * mismas acciones sobre el mismo GestorPedidos.
 */
public class PanelMapaMesas extends JPanel {

    private static final String CARTA_VACIA = "vacia";
    private static final String CARTA_OCUPADA = "ocupada";

    private final GestorPedidos gestor;
    private final Runnable onCambio;

    private final Map<Integer, TileMesa> tiles = new LinkedHashMap<>();
    private Integer mesaSeleccionada = null;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelDetalle = new JPanel(cardLayout);

    // --- Card "mesa libre": tomar pedido nuevo ---
    private final JLabel lblTituloVacia = EstiloUI.subtitulo("Selecciona una mesa");
    private final List<ItemPedido> carritoNuevo = new ArrayList<>();
    private final DefaultTableModel modeloCarritoNuevo = crearModeloSoloLectura();
    private final JLabel lblTotalNuevo = new JLabel("Total: $0");
    private final JComboBox<Plato> comboPlatoNuevo = new JComboBox<>(MenuRestaurante.PLATOS.toArray(new Plato[0]));
    private final JSpinner spinnerCantidadNuevo = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));

    // --- Card "mesa ocupada": editar pedido en curso ---
    private final JLabel lblTituloOcupada = EstiloUI.subtitulo("Pedido");
    private final JLabel lblEstadoOcupada = new JLabel(" ");
    private final DefaultTableModel modeloItemsOcupada = crearModeloSoloLectura();
    private final JTable tablaItemsOcupada = new JTable(modeloItemsOcupada);
    private final JLabel lblTotalOcupada = new JLabel("Total: $0");
    private final JComboBox<Plato> comboPlatoOcupada = new JComboBox<>(MenuRestaurante.PLATOS.toArray(new Plato[0]));
    private final JSpinner spinnerCantidadOcupada = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));

    public PanelMapaMesas(GestorPedidos gestor, Runnable onCambio) {
        this.gestor = gestor;
        this.onCambio = onCambio;

        setLayout(new BorderLayout(14, 14));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(EstiloUI.FONDO);

        add(construirMapa(), BorderLayout.WEST);
        add(construirDetalle(), BorderLayout.CENTER);

        refrescar();
    }

    private static DefaultTableModel crearModeloSoloLectura() {
        return new DefaultTableModel(new String[]{"Plato", "Cantidad", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    // ---------- Mapa (izquierda) ----------

    private JComponent construirMapa() {
        JPanel contenedor = new JPanel(new BorderLayout(8, 8));
        contenedor.setOpaque(false);
        contenedor.add(EstiloUI.titulo("Salón"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 5, 12, 12));
        grid.setOpaque(false);
        for (Integer mesa : MenuRestaurante.MESAS) {
            TileMesa tile = new TileMesa(mesa, this::seleccionarMesa);
            tiles.put(mesa, tile);
            grid.add(tile);
        }
        contenedor.add(grid, BorderLayout.CENTER);

        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        leyenda.setOpaque(false);
        leyenda.add(chipLeyenda("Libre", EstiloUI.LIBRE));
        leyenda.add(chipLeyenda("Creado", EstiloUI.CREADO));
        leyenda.add(chipLeyenda("En preparación", EstiloUI.EN_PREPARACION));
        leyenda.add(chipLeyenda("Listo", EstiloUI.LISTO));
        contenedor.add(leyenda, BorderLayout.SOUTH);

        return contenedor;
    }

    private JLabel chipLeyenda(String texto, Color color) {
        JLabel chip = new JLabel("\u25CF " + texto);
        chip.setForeground(color.darker());
        chip.setFont(EstiloUI.FUENTE_TEXTO);
        return chip;
    }

    // ---------- Detalle (derecha) ----------

    private JComponent construirDetalle() {
        panelDetalle.setOpaque(false);
        panelDetalle.add(construirCardVacia(), CARTA_VACIA);
        panelDetalle.add(construirCardOcupada(), CARTA_OCUPADA);

        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setBackground(EstiloUI.TARJETA);
        envoltorio.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        envoltorio.add(panelDetalle, BorderLayout.CENTER);
        return envoltorio;
    }

    private JPanel construirCardVacia() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.add(lblTituloVacia, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formulario.setOpaque(false);
        formulario.add(new JLabel("Plato:"));
        formulario.add(comboPlatoNuevo);
        formulario.add(new JLabel("Cantidad:"));
        formulario.add(spinnerCantidadNuevo);
        JButton btnAgregar = new JButton("Agregar +");
        btnAgregar.addActionListener(e -> agregarAlCarritoNuevo());
        formulario.add(btnAgregar);

        JTable tablaCarritoNuevo = new JTable(modeloCarritoNuevo);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.setOpaque(false);
        centro.add(formulario, BorderLayout.NORTH);
        centro.add(new JScrollPane(tablaCarritoNuevo), BorderLayout.CENTER);
        panel.add(centro, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        JButton btnQuitar = new JButton("Quitar línea");
        btnQuitar.addActionListener(e -> {
            int fila = tablaCarritoNuevo.getSelectedRow();
            if (fila >= 0) {
                carritoNuevo.remove(fila);
                refrescarCarritoNuevo();
            }
        });
        JButton btnConfirmar = new JButton("Confirmar pedido");
        btnConfirmar.addActionListener(e -> confirmarPedidoNuevo());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.setOpaque(false);
        botones.add(btnQuitar);
        botones.add(btnConfirmar);
        abajo.add(lblTotalNuevo, BorderLayout.WEST);
        abajo.add(botones, BorderLayout.EAST);
        panel.add(abajo, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel construirCardOcupada() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        JPanel encabezado = new JPanel(new GridLayout(2, 1));
        encabezado.setOpaque(false);
        encabezado.add(lblTituloOcupada);
        encabezado.add(lblEstadoOcupada);
        panel.add(encabezado, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formulario.setOpaque(false);
        formulario.add(new JLabel("Agregar plato:"));
        formulario.add(comboPlatoOcupada);
        formulario.add(new JLabel("Cantidad:"));
        formulario.add(spinnerCantidadOcupada);
        JButton btnAgregarItem = new JButton("Agregar +");
        btnAgregarItem.addActionListener(e -> agregarItemAPedidoActivo());
        formulario.add(btnAgregarItem);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.setOpaque(false);
        centro.add(formulario, BorderLayout.NORTH);
        centro.add(new JScrollPane(tablaItemsOcupada), BorderLayout.CENTER);
        panel.add(centro, BorderLayout.CENTER);

        JButton btnQuitarItem = new JButton("Quitar línea seleccionada");
        btnQuitarItem.addActionListener(e -> quitarItemDePedidoActivo());
        JButton btnAvanzar = new JButton("Avanzar estado ->");
        btnAvanzar.addActionListener(e -> avanzarPedidoActivo());
        JButton btnCancelar = new JButton("Cancelar pedido");
        btnCancelar.addActionListener(e -> cancelarPedidoActivo());

        JPanel botonesIzq = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botonesIzq.setOpaque(false);
        botonesIzq.add(btnQuitarItem);
        JPanel botonesDer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botonesDer.setOpaque(false);
        botonesDer.add(btnAvanzar);
        botonesDer.add(btnCancelar);

        JPanel filaBotones = new JPanel(new BorderLayout());
        filaBotones.setOpaque(false);
        filaBotones.add(botonesIzq, BorderLayout.WEST);
        filaBotones.add(botonesDer, BorderLayout.EAST);

        JPanel abajo = new JPanel(new BorderLayout(4, 4));
        abajo.setOpaque(false);
        abajo.add(lblTotalOcupada, BorderLayout.NORTH);
        abajo.add(filaBotones, BorderLayout.SOUTH);
        panel.add(abajo, BorderLayout.SOUTH);

        return panel;
    }

    // ---------- Lógica ----------

    private void seleccionarMesa(int mesa) {
        mesaSeleccionada = mesa;
        carritoNuevo.clear();
        refrescarCarritoNuevo();
        refrescar();
    }

    /** Busca el pedido no-terminal más reciente asociado a esa mesa (si existe). */
    private Pedido pedidoActivoDeMesa(int mesa) {
        Pedido resultado = null;
        for (Pedido p : gestor.getPedidos()) {
            if (p.getMesa() == mesa && p.estaEditable()) {
                if (resultado == null || p.getId() > resultado.getId()) {
                    resultado = p;
                }
            }
        }
        return resultado;
    }

    private void actualizarCardSegunMesa() {
        if (mesaSeleccionada == null) {
            return;
        }
        Pedido activo = pedidoActivoDeMesa(mesaSeleccionada);
        if (activo == null) {
            lblTituloVacia.setText("Mesa " + mesaSeleccionada + " \u2014 nuevo pedido");
            cardLayout.show(panelDetalle, CARTA_VACIA);
        } else {
            cargarPedidoEnCardOcupada(activo);
            cardLayout.show(panelDetalle, CARTA_OCUPADA);
        }
    }

    private void cargarPedidoEnCardOcupada(Pedido pedido) {
        lblTituloOcupada.setText("Mesa " + pedido.getMesa() + " \u2014 Pedido #" + pedido.getId());
        lblEstadoOcupada.setText("Estado: " + pedido.getEstadoNombre());
        modeloItemsOcupada.setRowCount(0);
        for (ItemPedido item : pedido.getItems()) {
            modeloItemsOcupada.addRow(new Object[]{
                    item.getPlato().getNombre(), item.getCantidad(), String.format("$%,.0f", item.subtotal())
            });
        }
        lblTotalOcupada.setText(String.format("Total: $%,.0f", pedido.calcularTotal()));
    }

    private void agregarAlCarritoNuevo() {
        Plato plato = (Plato) comboPlatoNuevo.getSelectedItem();
        int cantidad = (int) spinnerCantidadNuevo.getValue();
        if (plato == null) {
            return;
        }
        carritoNuevo.add(new ItemPedido(plato, cantidad));
        refrescarCarritoNuevo();
    }

    private void refrescarCarritoNuevo() {
        modeloCarritoNuevo.setRowCount(0);
        double total = 0;
        for (ItemPedido item : carritoNuevo) {
            modeloCarritoNuevo.addRow(new Object[]{
                    item.getPlato().getNombre(), item.getCantidad(), String.format("$%,.0f", item.subtotal())
            });
            total += item.subtotal();
        }
        lblTotalNuevo.setText(String.format("Total: $%,.0f", total));
    }

    private void confirmarPedidoNuevo() {
        if (mesaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una mesa primero.");
            return;
        }
        if (carritoNuevo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agrega al menos un plato antes de confirmar.");
            return;
        }
        Pedido pedido = gestor.crearPedido(mesaSeleccionada);
        for (ItemPedido item : carritoNuevo) {
            pedido.agregarItem(item);
        }
        carritoNuevo.clear();
        refrescarCarritoNuevo();
        notificarCambio();
    }

    private void agregarItemAPedidoActivo() {
        if (mesaSeleccionada == null) {
            return;
        }
        Pedido activo = pedidoActivoDeMesa(mesaSeleccionada);
        if (activo == null) {
            return;
        }
        Plato plato = (Plato) comboPlatoOcupada.getSelectedItem();
        int cantidad = (int) spinnerCantidadOcupada.getValue();
        activo.agregarItem(new ItemPedido(plato, cantidad));
        notificarCambio();
    }

    private void quitarItemDePedidoActivo() {
        if (mesaSeleccionada == null) {
            return;
        }
        Pedido activo = pedidoActivoDeMesa(mesaSeleccionada);
        if (activo == null) {
            return;
        }
        int fila = tablaItemsOcupada.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una línea del pedido para quitarla.");
            return;
        }
        if (activo.getItems().size() <= 1) {
            JOptionPane.showMessageDialog(this,
                    "El pedido debe tener al menos un plato. Si quieres vaciarlo del todo, cancélalo.");
            return;
        }
        ItemPedido item = activo.getItems().get(fila);
        activo.removerItem(item);
        notificarCambio();
    }

    private void avanzarPedidoActivo() {
        if (mesaSeleccionada == null) {
            return;
        }
        Pedido activo = pedidoActivoDeMesa(mesaSeleccionada);
        if (activo == null) {
            return;
        }
        gestor.avanzarEstado(activo);
        notificarCambio();
    }

    private void cancelarPedidoActivo() {
        if (mesaSeleccionada == null) {
            return;
        }
        Pedido activo = pedidoActivoDeMesa(mesaSeleccionada);
        if (activo == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "\u00bfCancelar el pedido #" + activo.getId() + " de la mesa " + activo.getMesa() + "?",
                "Confirmar cancelación", JOptionPane.YES_NO_OPTION);
        if (confirmacion == JOptionPane.YES_OPTION) {
            activo.cambiarEstado(new EstadoCancelado());
            notificarCambio();
        }
    }

    private void notificarCambio() {
        refrescar();
        if (onCambio != null) {
            onCambio.run();
        }
    }

    /** Recalcula color/subtítulo de cada mesa y refresca la card de detalle activa. */
    public void refrescar() {
        for (Integer mesa : MenuRestaurante.MESAS) {
            Pedido activo = pedidoActivoDeMesa(mesa);
            Color color = activo == null ? EstiloUI.LIBRE : EstiloUI.colorParaEstado(activo.getEstadoNombre());
            String subtitulo = activo == null
                    ? "Libre"
                    : activo.getEstadoNombre() + " \u00b7 $" + String.format("%,.0f", activo.calcularTotal());
            boolean seleccionada = mesaSeleccionada != null && mesaSeleccionada == mesa;
            tiles.get(mesa).actualizar(color, subtitulo, seleccionada);
        }
        actualizarCardSegunMesa();
    }
}
