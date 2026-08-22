package com.restaurant.reportes;

import com.restaurant.modelo.ItemPedido;
import com.restaurant.modelo.Pedido;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Cuenta cuántas unidades se pidieron de cada plato y ordena de mayor a menor. */
public class ReportePlatosMasPedidos implements EstrategiaReporte {

    @Override
    public String generar(List<Pedido> pedidos) {
        Map<String, Integer> conteo = contarUnidadesPorPlato(pedidos);

        StringBuilder sb = new StringBuilder("== Platos más pedidos ==\n");
        conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append(" unidades\n"));
        return sb.toString();
    }

    static Map<String, Integer> contarUnidadesPorPlato(List<Pedido> pedidos) {
        Map<String, Integer> conteo = new HashMap<>();
        for (Pedido pedido : pedidos) {
            for (ItemPedido item : pedido.getItems()) {
                conteo.merge(item.getPlato().getNombre(), item.getCantidad(), Integer::sum);
            }
        }
        return conteo;
    }
}
