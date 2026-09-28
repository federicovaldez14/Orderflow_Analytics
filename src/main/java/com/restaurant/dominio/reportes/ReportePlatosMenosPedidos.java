package com.restaurant.dominio.reportes;

import com.restaurant.dominio.modelo.Pedido;

import java.util.List;

/** Reutiliza el conteo de ReportePlatosMasPedidos pero ordena ascendente. */
public class ReportePlatosMenosPedidos implements EstrategiaReporte {

    @Override
    public Reporte generar(List<Pedido> pedidos) {
        return new Reporte("platos-menos-pedidos", "Platos menos pedidos", "unidades",
                ReportePlatosMasPedidos.ordenar(ReportePlatosMasPedidos.contarUnidadesPorPlato(pedidos), false));
    }
}
