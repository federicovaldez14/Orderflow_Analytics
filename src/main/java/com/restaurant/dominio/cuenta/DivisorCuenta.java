package com.restaurant.dominio.cuenta;

import com.restaurant.dominio.modelo.Pedido;

import java.util.ArrayList;
import java.util.List;

/**
 * Contexto del patrón Strategy: aplica la estrategia elegida y reparte la
 * propina voluntaria en proporción a lo que consumió cada persona.
 *
 * Reglas del negocio:
 *  - No se divide un pedido Cancelado ni uno sin consumo.
 *  - La propina es voluntaria: entre 0 % y 20 % del consumo, redondeada al peso.
 */
public final class DivisorCuenta {

    public static final int PROPINA_MAXIMA = 20;

    private DivisorCuenta() {
    }

    public static DivisionCuenta dividir(Pedido pedido, EstrategiaDivision estrategia, int propinaPorcentaje) {
        if (pedido == null || estrategia == null) {
            throw new IllegalArgumentException("Faltan el pedido o la forma de dividir");
        }
        if ("Cancelado".equals(pedido.getEstadoNombre())) {
            throw new IllegalStateException("El pedido #" + pedido.getId() + " está cancelado; no hay nada que cobrar");
        }
        if (pedido.calcularTotal() <= 0) {
            throw new IllegalStateException("El pedido #" + pedido.getId() + " no tiene consumo");
        }
        if (propinaPorcentaje < 0 || propinaPorcentaje > PROPINA_MAXIMA) {
            throw new IllegalArgumentException("La propina debe estar entre 0 y " + PROPINA_MAXIMA + " %: "
                    + propinaPorcentaje);
        }

        long subtotal = pedido.calcularTotal();
        List<ParteCuenta> consumo = estrategia.dividir(pedido);
        long propina = Math.round(subtotal * propinaPorcentaje / 100.0);

        List<ParteCuenta> partes = new ArrayList<>();
        if (propina == 0) {
            partes.addAll(consumo);
        } else {
            // Personas que no consumieron nada (peso 0) no pagan propina.
            List<Long> pesos = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < consumo.size(); i++) {
                if (consumo.get(i).consumo() > 0) {
                    pesos.add(consumo.get(i).consumo());
                    indices.add(i);
                }
            }
            List<Long> propinas = Repartidor.repartir(propina, pesos);
            long[] porPersona = new long[consumo.size()];
            for (int k = 0; k < indices.size(); k++) {
                porPersona[indices.get(k)] = propinas.get(k);
            }
            for (int i = 0; i < consumo.size(); i++) {
                ParteCuenta p = consumo.get(i);
                partes.add(new ParteCuenta(p.persona(), p.consumo(), porPersona[i], p.detalle()));
            }
        }
        return new DivisionCuenta(pedido.getId(), estrategia.nombre(), subtotal, propinaPorcentaje, propina, partes);
    }
}
