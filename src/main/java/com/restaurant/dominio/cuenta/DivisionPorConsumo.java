package com.restaurant.dominio.cuenta;

import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cada quien paga lo que consumió.
 *
 * Por cada línea del pedido se indica quiénes la consumieron y en qué
 * proporción ("peso"). Ejemplos sobre la línea "3x Bandeja Paisa":
 *   {Ana: 2, Luis: 1}  -> Ana paga 2 bandejas y Luis 1
 *   {Ana: 1, Luis: 1}  -> la compartieron: mitad y mitad
 *
 * Reglas: toda línea debe quedar asignada (si no, la cuenta no cuadraría),
 * las líneas deben existir y los pesos ser positivos.
 */
public class DivisionPorConsumo implements EstrategiaDivision {

    private final Map<Integer, Map<String, Integer>> asignaciones;
    private final List<String> personas;

    public DivisionPorConsumo(Map<Integer, Map<String, Integer>> asignaciones) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            throw new IllegalArgumentException("Asigna cada plato a al menos una persona");
        }
        Set<String> todas = new LinkedHashSet<>();
        Map<Integer, Map<String, Integer>> copia = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<String, Integer>> e : asignaciones.entrySet()) {
            Map<String, Integer> quienes = e.getValue();
            if (quienes == null || quienes.isEmpty()) {
                throw new IllegalArgumentException("La línea " + e.getKey() + " no tiene a nadie asignado");
            }
            Map<String, Integer> limpia = new LinkedHashMap<>();
            quienes.forEach((persona, peso) -> {
                if (persona == null || persona.isBlank()) {
                    throw new IllegalArgumentException("Cada persona debe tener un nombre");
                }
                if (peso == null || peso < 1) {
                    throw new IllegalArgumentException("La proporción de " + persona + " debe ser positiva: " + peso);
                }
                limpia.put(persona.trim(), peso);
                todas.add(persona.trim());
            });
            copia.put(e.getKey(), limpia);
        }
        Personas.validar(todas);
        this.asignaciones = copia;
        this.personas = List.copyOf(todas);
    }

    @Override
    public String nombre() {
        return "POR_CONSUMO";
    }

    @Override
    public List<ParteCuenta> dividir(Pedido pedido) {
        List<ItemPedido> items = pedido.getItems();
        for (Integer linea : asignaciones.keySet()) {
            if (linea == null || linea < 0 || linea >= items.size()) {
                throw new IllegalArgumentException("El pedido #" + pedido.getId() + " no tiene la línea " + linea);
            }
        }
        for (int i = 0; i < items.size(); i++) {
            if (!asignaciones.containsKey(i)) {
                throw new IllegalArgumentException("Falta asignar la línea " + i + " (" + items.get(i)
                        + "); la cuenta no cuadraría");
            }
        }

        Map<String, Long> consumo = new LinkedHashMap<>();
        Map<String, List<String>> detalle = new LinkedHashMap<>();
        personas.forEach(p -> {
            consumo.put(p, 0L);
            detalle.put(p, new ArrayList<>());
        });

        for (int i = 0; i < items.size(); i++) {
            ItemPedido item = items.get(i);
            Map<String, Integer> quienes = asignaciones.get(i);
            List<String> nombres = new ArrayList<>(quienes.keySet());
            List<Long> pesos = new ArrayList<>();
            int sumaPesos = 0;
            for (String n : nombres) {
                pesos.add((long) quienes.get(n));
                sumaPesos += quienes.get(n);
            }
            List<Long> montos = Repartidor.repartir(item.subtotal(), pesos);
            for (int k = 0; k < nombres.size(); k++) {
                String persona = nombres.get(k);
                consumo.merge(persona, montos.get(k), Long::sum);
                String fraccion = nombres.size() == 1 ? "" : quienes.get(persona) + "/" + sumaPesos + " de ";
                detalle.get(persona).add(fraccion + item + String.format(" ($%,d)", montos.get(k)));
            }
        }

        List<ParteCuenta> partes = new ArrayList<>();
        personas.forEach(p -> partes.add(new ParteCuenta(p, consumo.get(p), 0, detalle.get(p))));
        return partes;
    }
}
