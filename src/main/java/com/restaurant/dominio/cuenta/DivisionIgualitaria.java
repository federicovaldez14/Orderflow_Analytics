package com.restaurant.dominio.cuenta;

import com.restaurant.dominio.modelo.Pedido;

import java.util.ArrayList;
import java.util.List;

/** Todos pagan lo mismo (los pesos sobrantes del redondeo van a los primeros). */
public class DivisionIgualitaria implements EstrategiaDivision {

    private final List<String> personas;

    public DivisionIgualitaria(List<String> personas) {
        Personas.validar(personas);
        this.personas = List.copyOf(personas);
    }

    /** Atajo: "Persona 1", "Persona 2", ... */
    public static DivisionIgualitaria entre(int cantidad) {
        if (cantidad < 1 || cantidad > Personas.MAXIMO) {
            throw new IllegalArgumentException("Se puede dividir entre 1 y " + Personas.MAXIMO + " personas: " + cantidad);
        }
        List<String> nombres = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            nombres.add("Persona " + i);
        }
        return new DivisionIgualitaria(nombres);
    }

    @Override
    public String nombre() {
        return "IGUALITARIA";
    }

    @Override
    public List<ParteCuenta> dividir(Pedido pedido) {
        List<Long> montos = Repartidor.iguales(pedido.calcularTotal(), personas.size());
        List<ParteCuenta> partes = new ArrayList<>();
        for (int i = 0; i < personas.size(); i++) {
            partes.add(new ParteCuenta(personas.get(i).trim(), montos.get(i), 0,
                    List.of("1/" + personas.size() + " del total")));
        }
        return partes;
    }
}
