package com.restaurant.dominio.reportes;

import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unidades vendidas por plato, de mayor a menor.
 * Corte 2: los pedidos cancelados ya no cuentan (no se vendieron). A igual
 * cantidad se ordena por nombre, para que el resultado sea estable.
 */
public class ReportePlatosMasPedidos implements EstrategiaReporte {

    @Override
    public Reporte generar(List<Pedido> pedidos) {
        return new Reporte("platos-mas-pedidos", "Platos más pedidos", "unidades",
                ordenar(contarUnidadesPorPlato(pedidos), true));
    }

    static Map<String, Integer> contarUnidadesPorPlato(List<Pedido> pedidos) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (Pedido pedido : pedidos) {
            if ("Cancelado".equals(pedido.getEstadoNombre())) {
                continue;
            }
            for (ItemPedido item : pedido.getItems()) {
                conteo.merge(item.getPlato().getNombre(), item.getCantidad(), Integer::sum);
            }
        }
        return conteo;
    }

    static List<Reporte.Dato> ordenar(Map<String, Integer> conteo, boolean descendente) {
        Comparator<Map.Entry<String, Integer>> porValor = Map.Entry.comparingByValue();
        if (descendente) {
            porValor = porValor.reversed();
        }
        return conteo.entrySet().stream()
                .sorted(porValor.thenComparing(Map.Entry.comparingByKey()))
                .map(e -> new Reporte.Dato(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
