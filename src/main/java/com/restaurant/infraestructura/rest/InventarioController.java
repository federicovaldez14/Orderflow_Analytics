package com.restaurant.infraestructura.rest;

import com.restaurant.aplicacion.casodeuso.ServicioInventario;
import com.restaurant.aplicacion.excepcion.RecursoNoEncontradoException;
import com.restaurant.dominio.inventario.MovimientoInventario;
import com.restaurant.dominio.inventario.Receta;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Adaptador REST del Reto 1 (inventario con trazabilidad). */
@RestController
@RequestMapping("/api")
public class InventarioController {

    private final ServicioInventario inventario;

    public InventarioController(ServicioInventario inventario) {
        this.inventario = inventario;
    }

    @GetMapping("/inventario")
    public List<Dtos.IngredienteDto> ingredientes() {
        return inventario.ingredientes().stream().map(Dtos.IngredienteDto::de).collect(Collectors.toList());
    }

    @GetMapping("/inventario/alertas")
    public List<Dtos.IngredienteDto> bajoMinimo() {
        return inventario.bajoMinimo().stream().map(Dtos.IngredienteDto::de).collect(Collectors.toList());
    }

    /** Historial (trazabilidad). ?ingrediente=ARROZ para filtrar; ?limite=50 por defecto. */
    @GetMapping("/inventario/movimientos")
    public List<MovimientoInventario> movimientos(@RequestParam(value = "ingrediente", required = false) String codigo,
                                                  @RequestParam(value = "limite", defaultValue = "50") int limite) {
        return inventario.movimientos(codigo, limite);
    }

    /** Qué ingredientes consumió (o devolvió) un pedido concreto. */
    @GetMapping("/pedidos/{id}/inventario")
    public List<MovimientoInventario> movimientosDePedido(@PathVariable int id) {
        return inventario.movimientosDePedido(id);
    }

    @PostMapping("/inventario/{codigo}/reposicion")
    public Dtos.IngredienteDto reponer(@PathVariable String codigo, @RequestBody Dtos.CantidadDto cuerpo) {
        return Dtos.IngredienteDto.de(inventario.reponer(codigo, cuerpo.cantidad(), cuerpo.nota()));
    }

    @PostMapping("/inventario/{codigo}/ajuste")
    public Dtos.IngredienteDto ajustar(@PathVariable String codigo, @RequestBody Dtos.ConteoDto cuerpo) {
        return Dtos.IngredienteDto.de(inventario.ajustarConteo(codigo, cuerpo.stockContado(), cuerpo.nota()));
    }

    @GetMapping("/recetas/{plato}")
    public Map<String, Long> receta(@PathVariable String plato) {
        return inventario.recetaDe(plato).map(Receta::getPorPorcion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay receta para " + plato));
    }

    /** Porciones que alcanzan a prepararse de cada plato con el stock actual. */
    @GetMapping("/menu/disponibilidad")
    public Map<String, Long> disponibilidad() {
        return inventario.porcionesDisponibles();
    }
}
