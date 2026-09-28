package com.restaurant.infraestructura.ui;

import com.restaurant.aplicacion.casodeuso.GestorPedidos;
import com.restaurant.aplicacion.casodeuso.LineaSolicitada;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.modelo.Plato;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Vista de "mapa del salón": una cuadrícula de mesas coloreada según su
 * estado. Al hacer clic en una mesa se abre, a la derecha, el formulario
 * para tomar un pedido nuevo (mesa libre) o el detalle editable del pedido
 * en curso (mesa ocupada).
 *
 * Corte 2: esta clase es un ADAPTADOR DE ENTRADA. Ya no modifica objetos
 * Pedido directamente: toda acción pasa por el caso de uso GestorPedidos
 * (crearPedido, agregarItem, quitarLinea, avanzarEstado, cancelar). Así las
 * reglas se cumplen igual si la acción llega por Swing o por la API REST.
 */
public class PanelMapaMesas extends JPanel {

    private static final String CARTA_VACIA = "vacia";
    private static final String CARTA_OCUPADA = "ocupada";

    private final GestorPedidos gestor;
    private final Runnable onCambio;
    private final AccionesCuenta accionesCuenta;

    private final Map<Integer, TileMesa> tiles = new LinkedHashMap<>();
    private Integer mesaSeleccionada = null;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelDetalle = new JPanel(cardLayout);

    // --- Card "mesa libre": tomar pedido nuevo ---
    private final JLabel lblTituloVacia = EstiloUI.subtitulo("Selecciona una mesa");
    private final List<ItemPedido> carritoNuevo = new ArrayList<>();
    private final DefaultTableModel modeloCarritoNuevo = crearModeloSoloLectura();
    private final JLabel lblTotalNuevo = new JLabel("Total: $0");
    private final JComboBox<Plato> comboPlatoNuevo = new JComboBox<>();
    private final JSpinner spinnerCantidadNuevo = new JSpinner(new SpinnerNumberModel(1, 1, ItemPedido.CANTIDAD_MAXIMA, 1));

    // --- Card "mesa ocupada": editar pedido en curso ---
    private final JLabel lblTituloOcupada = EstiloUI.subtitulo("Pedido");
    private final JLabel lblEstadoOcupada = new JLabel(" ");
    private final DefaultTableModel modeloItemsOcupada = crearModeloSoloLectura();
    private final JTable tablaItemsOcupada = new JTable(modeloItemsOcupada);
    private final JLabel lblTotalOcupada = new JLabel("Total: $0");
    private final JComboBox<Plato> comboPlatoOcupada = new JComboBox<>();
    private final JSpinner spinnerCantidadOcupada = new JSpinner(new SpinnerNumberModel(1, 1, ItemPedido.CANTIDAD_MAXIMA, 1));
    private final JButton btnDividirCuenta = new JButton("Dividir cuenta...");

    /** Acción opcional para abrir la división de cuenta (Reto 2); null si no está disponible. */
    public interface AccionesCuenta {
        void dividirCuenta(Component padre, int pedidoId);
    }

    public PanelMapaMesas(GestorPedidos gestor, Runnable onCambio, AccionesCuenta accionesCuenta) {
        this.gestor = gestor;
        this.onCambio = onCambio;
        this.accionesCuenta = accionesCuenta;

        setLayout(new BorderLayout(14, 14));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(EstiloUI.FONDO);

        for (Plato plato : gestor.menu()) {
            comboPlatoNuevo.addItem(plato);
            comboPlatoOcupada.addItem(plato);
        }

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

        int mesas = gestor.getNumeroMesas();
        int columnas = Math.min(5, mesas);
        int filas = (int) Math.ceil(mesas / (double) columnas);
        JPanel grid = new JPanel(new GridLayout(filas, columnas, 12, 12));
        grid.setOpaque(false);
        for (int mesa = 1; mesa <= mesas; mesa++) {
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
        JLabel chip = new JLabel("● " + texto);
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
        btnDividirCuenta.addActionListener(e -> pedidoActivo().ifPresent(
                p -> accionesCuenta.dividirCuenta(this, p.getId())));
        btnDividirCuenta.setVisible(accionesCuenta != null);

        JPanel botonesIzq = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botonesIzq.setOpaque(false);
        botonesIzq.add(btnQuitarItem);
        botonesIzq.add(btnDividirCuenta);
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

    // ---------- Lógica (delegada al caso de uso) ----------

    private void seleccionarMesa(int mesa) {
        mesaSeleccionada = mesa;
        carritoNuevo.clear();
        refrescarCarritoNuevo();
        refrescar();
    }

    private Optional<Pedido> pedidoActivo() {
        return mesaSeleccionada == null ? Optional.empty() : gestor.pedidoActivoDeMesa(mesaSeleccionada);
    }

    private void actualizarCardSegunMesa(Map<Integer, Pedido> activos) {
        if (mesaSeleccionada == null) {
            return;
        }
        Pedido activo = activos.get(mesaSeleccionada);
        if (activo == null) {
            lblTituloVacia.setText("Mesa " + mesaSeleccionada + " — nuevo pedido");
            cardLayout.show(panelDetalle, CARTA_VACIA);
        } else {
            cargarPedidoEnCardOcupada(activo);
            cardLayout.show(panelDetalle, CARTA_OCUPADA);
        }
    }

    private void cargarPedidoEnCardOcupada(Pedido pedido) {
        lblTituloOcupada.setText("Mesa " + pedido.getMesa() + " — Pedido #" + pedido.getId());
        lblEstadoOcupada.setText("Estado: " + pedido.getEstadoNombre());
        int seleccion = tablaItemsOcupada.getSelectedRow();
        modeloItemsOcupada.setRowCount(0);
        for (ItemPedido item : pedido.getItems()) {
            modeloItemsOcupada.addRow(new Object[]{
                    item.getPlato().getNombre(), item.getCantidad(), EstiloUI.pesos(item.subtotal())
            });
        }
        if (seleccion >= 0 && seleccion < modeloItemsOcupada.getRowCount()) {
            tablaItemsOcupada.setRowSelectionInterval(seleccion, seleccion);
        }
        lblTotalOcupada.setText("Total: " + EstiloUI.pesos(pedido.calcularTotal()));
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
        long total = 0;
        for (ItemPedido item : carritoNuevo) {
            modeloCarritoNuevo.addRow(new Object[]{
                    item.getPlato().getNombre(), item.getCantidad(), EstiloUI.pesos(item.subtotal())
            });
            total += item.subtotal();
        }
        lblTotalNuevo.setText("Total: " + EstiloUI.pesos(total));
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
        List<LineaSolicitada> lineas = carritoNuevo.stream()
                .map(i -> new LineaSolicitada(i.getPlato().getNombre(), i.getCantidad()))
                .collect(Collectors.toList());
        if (EstiloUI.intentar(this, () -> gestor.crearPedido(mesaSeleccionada, lineas))) {
            carritoNuevo.clear();
            refrescarCarritoNuevo();
        }
        notificarCambio();
    }

    private void agregarItemAPedidoActivo() {
        Plato plato = (Plato) comboPlatoOcupada.getSelectedItem();
        int cantidad = (int) spinnerCantidadOcupada.getValue();
        pedidoActivo().ifPresent(p -> EstiloUI.intentar(this,
                () -> gestor.agregarItem(p.getId(), new LineaSolicitada(plato.getNombre(), cantidad))));
        notificarCambio();
    }

    private void quitarItemDePedidoActivo() {
        int fila = tablaItemsOcupada.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una línea del pedido para quitarla.");
            return;
        }
        pedidoActivo().ifPresent(p -> EstiloUI.intentar(this, () -> gestor.quitarLinea(p.getId(), fila)));
        notificarCambio();
    }

    private void avanzarPedidoActivo() {
        pedidoActivo().ifPresent(p -> EstiloUI.intentar(this, () -> gestor.avanzarEstado(p.getId())));
        notificarCambio();
    }

    private void cancelarPedidoActivo() {
        pedidoActivo().ifPresent(p -> {
            int confirmacion = JOptionPane.showConfirmDialog(this,
                    "¿Cancelar el pedido #" + p.getId() + " de la mesa " + p.getMesa() + "?",
                    "Confirmar cancelación", JOptionPane.YES_NO_OPTION);
            if (confirmacion == JOptionPane.YES_OPTION) {
                EstiloUI.intentar(this, () -> gestor.cancelar(p.getId()));
            }
        });
        notificarCambio();
    }

    private void notificarCambio() {
        refrescar();
        if (onCambio != null) {
            onCambio.run();
        }
    }

    /** Recalcula color/subtítulo de cada mesa y refresca la card de detalle activa. */
    public void refrescar() {
        Map<Integer, Pedido> activos = new LinkedHashMap<>();
        for (Pedido p : gestor.listar()) {
            if (p.estaEditable()) {
                activos.put(p.getMesa(), p);
            }
        }
        for (Map.Entry<Integer, TileMesa> e : tiles.entrySet()) {
            int mesa = e.getKey();
            Pedido activo = activos.get(mesa);
            Color color = activo == null ? EstiloUI.LIBRE : EstiloUI.colorParaEstado(activo.getEstadoNombre());
            String subtitulo = activo == null
                    ? "Libre"
                    : activo.getEstadoNombre() + " · " + EstiloUI.pesos(activo.calcularTotal());
            boolean seleccionada = mesaSeleccionada != null && mesaSeleccionada == mesa;
            e.getValue().actualizar(color, subtitulo, seleccionada);
        }
        actualizarCardSegunMesa(activos);
    }
}
