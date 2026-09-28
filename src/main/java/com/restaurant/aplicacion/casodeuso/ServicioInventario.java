package com.restaurant.aplicacion.casodeuso;

import com.restaurant.aplicacion.excepcion.RecursoNoEncontradoException;
import com.restaurant.aplicacion.excepcion.ReglaNegocioException;
import com.restaurant.aplicacion.puerto.salida.AlertaInventario;
import com.restaurant.aplicacion.puerto.salida.InventarioRepositorio;
import com.restaurant.aplicacion.puerto.salida.InventarioRepositorio.Motivo;
import com.restaurant.dominio.inventario.CalculadoraConsumo;
import com.restaurant.dominio.inventario.Ingrediente;
import com.restaurant.dominio.inventario.MovimientoInventario;
import com.restaurant.dominio.inventario.PoliticaDevolucion;
import com.restaurant.dominio.inventario.Receta;
import com.restaurant.dominio.inventario.TipoMovimiento;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Caso de uso del RETO 1: inventario con trazabilidad.
 *
 * - Al tomar un pedido descuenta los ingredientes según la receta de cada
 *   plato; si algo no alcanza, el pedido NO se crea (StockInsuficienteException).
 * - Al quitar platos o cancelar aplica la PoliticaDevolucion (devolución o merma).
 * - Cada cambio deja un MovimientoInventario (qué, cuánto, por qué, pedido,
 *   stock resultante): esa es la trazabilidad.
 * - Cuando un ingrediente cruza su mínimo avisa por el puerto AlertaInventario.
 */
public class ServicioInventario implements ControlInventario {

    private final InventarioRepositorio repo;
    private final List<AlertaInventario> alertas;
    private final Clock reloj;

    public ServicioInventario(InventarioRepositorio repo, List<AlertaInventario> alertas, Clock reloj) {
        this.repo = repo;
        this.alertas = List.copyOf(alertas);
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------
    // Integración con pedidos (ControlInventario)
    // ------------------------------------------------------------------

    @Override
    public void reservar(Pedido pedido, List<ItemPedido> nuevos) {
        Map<String, Long> consumo = CalculadoraConsumo.calcular(nuevos, repo.recetasPorPlato());
        if (consumo.isEmpty()) {
            return;
        }
        Map<String, Long> resultantes = repo.descontar(consumo,
                new Motivo(TipoMovimiento.SALIDA_VENTA, pedido.getId(),
                        "Venta pedido #" + pedido.getId() + " mesa " + pedido.getMesa(), ahora()));
        avisarCruces(consumo, resultantes);
    }

    @Override
    public void liberar(Pedido pedido, List<ItemPedido> quitados, String estadoAntes) {
        Map<String, Long> consumo = CalculadoraConsumo.calcular(quitados, repo.recetasPorPlato());
        if (consumo.isEmpty()) {
            return;
        }
        TipoMovimiento tipo = PoliticaDevolucion.tipoPara(estadoAntes);
        Motivo motivo = new Motivo(tipo, pedido.getId(),
                (tipo == TipoMovimiento.DEVOLUCION ? "Devolución" : "Merma") + " pedido #" + pedido.getId()
                        + " (estaba '" + estadoAntes + "')", ahora());
        if (tipo == TipoMovimiento.DEVOLUCION) {
            repo.sumar(consumo, motivo);
        } else {
            repo.registrarSinCambio(consumo, motivo);
        }
    }

    // ------------------------------------------------------------------
    // Operaciones del administrador
    // ------------------------------------------------------------------

    /** Entrada de mercancía (compra al proveedor). */
    public Ingrediente reponer(String codigo, long cantidad, String nota) {
        Ingrediente ing = obtener(codigo);
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a reponer debe ser mayor que cero: " + cantidad);
        }
        repo.sumar(Map.of(ing.getCodigo(), cantidad),
                new Motivo(TipoMovimiento.ENTRADA, null, textoONulo(nota, "Reposición"), ahora()));
        return obtener(codigo);
    }

    /**
     * Conteo físico: el stock real es "stockContado". Se registra la
     * diferencia como AJUSTE (positiva o negativa) para no perder el rastro.
     */
    public Ingrediente ajustarConteo(String codigo, long stockContado, String nota) {
        if (stockContado < 0) {
            throw new IllegalArgumentException("El conteo físico no puede ser negativo: " + stockContado);
        }
        Ingrediente ing = obtener(codigo);
        long diferencia = stockContado - ing.getStock();
        if (diferencia == 0) {
            throw new ReglaNegocioException("El conteo coincide con el sistema; no hay nada que ajustar.");
        }
        Motivo motivo = new Motivo(TipoMovimiento.AJUSTE, null,
                textoONulo(nota, "Conteo físico") + " (sistema " + ing.getStock() + ", real " + stockContado + ")", ahora());
        if (diferencia > 0) {
            repo.sumar(Map.of(ing.getCodigo(), diferencia), motivo);
        } else {
            Map<String, Long> resultado = repo.descontar(Map.of(ing.getCodigo(), -diferencia), motivo);
            avisarCruces(Map.of(ing.getCodigo(), -diferencia), resultado);
        }
        return obtener(codigo);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public List<Ingrediente> ingredientes() {
        return repo.listarIngredientes();
    }

    public List<Ingrediente> bajoMinimo() {
        return repo.listarIngredientes().stream().filter(Ingrediente::bajoMinimo).collect(Collectors.toList());
    }

    public List<MovimientoInventario> movimientos(String codigo, int limite) {
        if (limite < 1 || limite > 1000) {
            throw new IllegalArgumentException("El límite debe estar entre 1 y 1000: " + limite);
        }
        return repo.movimientos(codigo == null || codigo.isBlank() ? null : codigo.trim().toUpperCase(), limite);
    }

    public List<MovimientoInventario> movimientosDePedido(int pedidoId) {
        return repo.movimientosDePedido(pedidoId);
    }

    public Optional<Receta> recetaDe(String plato) {
        return Optional.ofNullable(repo.recetasPorPlato().get(plato));
    }

    /** Cuántas porciones alcanzan de cada plato con recetas registradas. */
    public Map<String, Long> porcionesDisponibles() {
        Map<String, Long> stock = repo.listarIngredientes().stream()
                .collect(Collectors.toMap(Ingrediente::getCodigo, Ingrediente::getStock));
        Map<String, Long> resultado = new LinkedHashMap<>();
        repo.recetasPorPlato().forEach((plato, receta) ->
                resultado.put(plato, CalculadoraConsumo.porcionesDisponibles(receta, stock)));
        return resultado;
    }

    // ------------------------------------------------------------------

    private Ingrediente obtener(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código del ingrediente es obligatorio");
        }
        return repo.buscarIngrediente(codigo.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el ingrediente " + codigo));
    }

    private void avisarCruces(Map<String, Long> consumo, Map<String, Long> resultantes) {
        if (alertas.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Long> e : resultantes.entrySet()) {
            long despues = e.getValue();
            long antes = despues + consumo.getOrDefault(e.getKey(), 0L);
            repo.buscarIngrediente(e.getKey()).ifPresent(ing -> {
                if (Ingrediente.cruzoMinimo(antes, despues, ing.getStockMinimo())) {
                    alertas.forEach(a -> a.stockBajo(ing));
                }
            });
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private static String textoONulo(String texto, String porDefecto) {
        return texto == null || texto.isBlank() ? porDefecto : texto.trim();
    }
}
