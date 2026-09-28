package com.restaurant.infraestructura.ui;

import com.restaurant.aplicacion.casodeuso.GestorPedidos;
import com.restaurant.aplicacion.casodeuso.ServicioCuenta;
import com.restaurant.dominio.cuenta.DivisionCuenta;
import com.restaurant.dominio.cuenta.DivisionIgualitaria;
import com.restaurant.dominio.cuenta.DivisionPorConsumo;
import com.restaurant.dominio.cuenta.DivisionPorPorcentaje;
import com.restaurant.dominio.cuenta.DivisorCuenta;
import com.restaurant.dominio.cuenta.EstrategiaDivision;
import com.restaurant.dominio.cuenta.ParteCuenta;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Diálogo del Reto 2: dividir la cuenta de un pedido. Tres pestañas, una por
 * estrategia (Strategy). La UI solo arma la estrategia con lo que escribe el
 * mesero; el cálculo y la validación (que todo cuadre al peso) son del dominio.
 */
public class DialogoDividirCuenta extends JDialog {

    private final ServicioCuenta servicio;
    private final Pedido pedido;

    private final JTabbedPane pestanas = new JTabbedPane();
    private final JSpinner spinnerPropina = new JSpinner(new SpinnerNumberModel(0, 0, DivisorCuenta.PROPINA_MAXIMA, 1));
    private final DefaultTableModel modeloResultado = new DefaultTableModel(
            new String[]{"Persona", "Consumo", "Propina", "Paga", "Detalle"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JLabel lblVerificacion = new JLabel(" ");

    // Pestaña 1: iguales
    private final JSpinner spinnerPersonas = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
    private final JTextField txtNombresIguales = new JTextField(28);

    // Pestaña 2: por consumo
    private final JTextField txtPersonasConsumo = new JTextField("Persona 1, Persona 2", 28);
    private DefaultTableModel modeloConsumo;
    private final JTable tablaConsumo = new JTable();

    // Pestaña 3: por porcentaje
    private final DefaultTableModel modeloPorcentaje = new DefaultTableModel(new Object[][]{{"Persona 1", 50}, {"Persona 2", 50}},
            new String[]{"Persona", "%"}) {
        @Override
        public Class<?> getColumnClass(int c) {
            return c == 1 ? Integer.class : String.class;
        }
    };

    public DialogoDividirCuenta(Window padre, GestorPedidos gestor, ServicioCuenta servicio, int pedidoId) {
        super(padre, "Dividir cuenta — pedido #" + pedidoId, ModalityType.APPLICATION_MODAL);
        this.servicio = servicio;
        this.pedido = gestor.obtener(pedidoId);

        setLayout(new BorderLayout(8, 8));
        JLabel encabezado = EstiloUI.subtitulo("Mesa " + pedido.getMesa() + " · " + pedido.getEstadoNombre()
                + " · Consumo " + EstiloUI.pesos(pedido.calcularTotal()));
        encabezado.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        add(encabezado, BorderLayout.NORTH);

        pestanas.addTab("Partes iguales", pestanaIguales());
        pestanas.addTab("Por lo que consumió cada uno", pestanaConsumo());
        pestanas.addTab("Por porcentaje", pestanaPorcentaje());

        JPanel resultado = new JPanel(new BorderLayout(4, 4));
        resultado.setBorder(BorderFactory.createTitledBorder("Resultado"));
        JTable tablaResultado = new JTable(modeloResultado);
        tablaResultado.getColumnModel().getColumn(4).setPreferredWidth(360);
        resultado.add(new JScrollPane(tablaResultado), BorderLayout.CENTER);
        resultado.add(lblVerificacion, BorderLayout.SOUTH);

        JSplitPane centro = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pestanas, resultado);
        centro.setResizeWeight(0.5);
        add(centro, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        abajo.add(new JLabel("Propina voluntaria (%):"));
        abajo.add(spinnerPropina);
        JButton btnCalcular = new JButton("Calcular división");
        btnCalcular.addActionListener(e -> calcular());
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        abajo.add(btnCalcular);
        abajo.add(btnCerrar);
        add(abajo, BorderLayout.SOUTH);

        setSize(820, 620);
        setLocationRelativeTo(padre);
    }

    // ---------------- Pestañas ----------------

    private JPanel pestanaIguales() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Número de personas:"), c);
        c.gridx = 1;
        p.add(spinnerPersonas, c);
        c.gridx = 0;
        c.gridy = 1;
        p.add(new JLabel("Nombres (opcional, separados por coma):"), c);
        c.gridx = 1;
        p.add(txtNombresIguales, c);
        return p;
    }

    private JPanel pestanaConsumo() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
        arriba.add(new JLabel("Personas:"));
        arriba.add(txtPersonasConsumo);
        JButton aplicar = new JButton("Armar tabla");
        aplicar.addActionListener(e -> armarTablaConsumo());
        arriba.add(aplicar);
        p.add(arriba, BorderLayout.NORTH);
        p.add(new JScrollPane(tablaConsumo), BorderLayout.CENTER);
        p.add(new JLabel("<html>Escribe cuántas unidades (o partes) de cada plato consumió cada persona. "
                + "0 = no consumió; 1 y 1 = lo compartieron a medias.</html>"), BorderLayout.SOUTH);
        armarTablaConsumo();
        return p;
    }

    private final JTable tablaPorcentaje = new JTable(modeloPorcentaje);

    private JPanel pestanaPorcentaje() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        JTable tabla = tablaPorcentaje;
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton agregar = new JButton("Agregar persona");
        agregar.addActionListener(e -> modeloPorcentaje.addRow(
                new Object[]{"Persona " + (modeloPorcentaje.getRowCount() + 1), 0}));
        JButton quitar = new JButton("Quitar seleccionada");
        quitar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila >= 0) {
                modeloPorcentaje.removeRow(fila);
            }
        });
        botones.add(agregar);
        botones.add(quitar);
        botones.add(new JLabel("Los porcentajes deben sumar 100."));
        p.add(botones, BorderLayout.SOUTH);
        return p;
    }

    private void armarTablaConsumo() {
        List<String> personas = nombres(txtPersonasConsumo.getText());
        if (personas.isEmpty()) {
            personas = List.of("Persona 1");
        }
        List<String> columnas = new ArrayList<>(List.of("Línea", "Plato", "Subtotal"));
        columnas.addAll(personas);
        final int fijas = 3;
        modeloConsumo = new DefaultTableModel(columnas.toArray(), 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c >= fijas;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c >= fijas ? Integer.class : Object.class;
            }
        };
        List<ItemPedido> items = pedido.getItems();
        for (int i = 0; i < items.size(); i++) {
            Object[] fila = new Object[columnas.size()];
            fila[0] = i;
            fila[1] = items.get(i).toString();
            fila[2] = EstiloUI.pesos(items.get(i).subtotal());
            for (int k = fijas; k < fila.length; k++) {
                // Por defecto todo se reparte entre todos (el mesero ajusta después).
                fila[k] = 1;
            }
            modeloConsumo.addRow(fila);
        }
        tablaConsumo.setModel(modeloConsumo);
    }

    // ---------------- Cálculo ----------------

    private void calcular() {
        for (JTable t : new JTable[]{tablaConsumo, tablaPorcentaje}) {
            if (t.isEditing()) {
                t.getCellEditor().stopCellEditing();
            }
        }
        Supplier<EstrategiaDivision> fabrica;
        switch (pestanas.getSelectedIndex()) {
            case 0:
                fabrica = this::estrategiaIguales;
                break;
            case 1:
                fabrica = this::estrategiaConsumo;
                break;
            default:
                fabrica = this::estrategiaPorcentaje;
        }
        int propina = (int) spinnerPropina.getValue();
        EstiloUI.intentar(this, () -> mostrar(servicio.dividir(pedido.getId(), fabrica.get(), propina)));
    }

    private EstrategiaDivision estrategiaIguales() {
        List<String> nombres = nombres(txtNombresIguales.getText());
        return nombres.isEmpty() ? DivisionIgualitaria.entre((int) spinnerPersonas.getValue())
                : new DivisionIgualitaria(nombres);
    }

    private EstrategiaDivision estrategiaConsumo() {
        Map<Integer, Map<String, Integer>> asignaciones = new LinkedHashMap<>();
        for (int fila = 0; fila < modeloConsumo.getRowCount(); fila++) {
            Map<String, Integer> quienes = new LinkedHashMap<>();
            for (int col = 3; col < modeloConsumo.getColumnCount(); col++) {
                Object v = modeloConsumo.getValueAt(fila, col);
                int peso = v == null ? 0 : (Integer) v;
                if (peso > 0) {
                    quienes.put(modeloConsumo.getColumnName(col), peso);
                }
            }
            if (!quienes.isEmpty()) {
                asignaciones.put(fila, quienes);
            }
        }
        return new DivisionPorConsumo(asignaciones);
    }

    private EstrategiaDivision estrategiaPorcentaje() {
        Map<String, Integer> porcentajes = new LinkedHashMap<>();
        for (int fila = 0; fila < modeloPorcentaje.getRowCount(); fila++) {
            Object nombre = modeloPorcentaje.getValueAt(fila, 0);
            Object pct = modeloPorcentaje.getValueAt(fila, 1);
            porcentajes.put(nombre == null ? "" : nombre.toString(), pct == null ? 0 : (Integer) pct);
        }
        return new DivisionPorPorcentaje(porcentajes);
    }

    private void mostrar(DivisionCuenta division) {
        modeloResultado.setRowCount(0);
        long suma = 0;
        for (ParteCuenta p : division.partes()) {
            modeloResultado.addRow(new Object[]{p.persona(), EstiloUI.pesos(p.consumo()), EstiloUI.pesos(p.propina()),
                    EstiloUI.pesos(p.total()), String.join(" · ", p.detalle())});
            suma += p.total();
        }
        lblVerificacion.setText("Total a cobrar " + EstiloUI.pesos(division.total()) + " (consumo "
                + EstiloUI.pesos(division.subtotal()) + " + propina " + EstiloUI.pesos(division.propina())
                + ")  =  suma de las partes " + EstiloUI.pesos(suma) + "  ✔");
    }

    private static List<String> nombres(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
