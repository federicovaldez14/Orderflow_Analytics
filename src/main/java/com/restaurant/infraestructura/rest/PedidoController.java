package com.restaurant.infraestructura.rest;

import com.restaurant.aplicacion.casodeuso.GestorPedidos;
import com.restaurant.aplicacion.casodeuso.LineaSolicitada;
import com.restaurant.dominio.modelo.Pedido;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptador de entrada REST. No contiene reglas: traduce HTTP/JSON a
 * llamadas al caso de uso GestorPedidos y el resultado a JSON. Es la misma
 * puerta que usan k6 (pruebas de carga) y las pruebas de caja negra.
 */
@RestController
@RequestMapping("/api")
public class PedidoController {

    private final GestorPedidos gestor;

    public PedidoController(GestorPedidos gestor) {
        this.gestor = gestor;
    }

    @GetMapping("/menu")
    public List<Dtos.PlatoDto> menu() {
        return gestor.menu().stream().map(Dtos.PlatoDto::de).collect(Collectors.toList());
    }

    @GetMapping("/mesas")
    public List<Dtos.MesaDto> mesas() {
        List<Dtos.MesaDto> mesas = new ArrayList<>();
        for (int m = 1; m <= gestor.getNumeroMesas(); m++) {
            final int mesa = m;
            mesas.add(gestor.pedidoActivoDeMesa(mesa)
                    .map(p -> new Dtos.MesaDto(mesa, false, p.getId(), p.getEstadoNombre(), p.calcularTotal()))
                    .orElse(new Dtos.MesaDto(mesa, true, null, "Libre", 0)));
        }
        return mesas;
    }

    @GetMapping("/pedidos")
    public List<Dtos.PedidoDto> listar() {
        return gestor.listar().stream().map(Dtos.PedidoDto::de).collect(Collectors.toList());
    }

    @GetMapping("/pedidos/{id}")
    public Dtos.PedidoDto obtener(@PathVariable int id) {
        return Dtos.PedidoDto.de(gestor.obtener(id));
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.PedidoDto crear(@RequestBody Dtos.CrearPedidoDto solicitud) {
        List<LineaSolicitada> lineas = solicitud.lineas() == null ? List.of()
                : solicitud.lineas().stream()
                    .map(l -> new LineaSolicitada(l.plato(), l.cantidad()))
                    .collect(Collectors.toList());
        Pedido pedido = gestor.crearPedido(solicitud.mesa(), lineas);
        return Dtos.PedidoDto.de(pedido);
    }

    @PostMapping("/pedidos/{id}/items")
    public Dtos.PedidoDto agregarItem(@PathVariable int id, @RequestBody Dtos.LineaDto linea) {
        return Dtos.PedidoDto.de(gestor.agregarItem(id, new LineaSolicitada(linea.plato(), linea.cantidad())));
    }

    @DeleteMapping("/pedidos/{id}/items/{linea}")
    public Dtos.PedidoDto quitarLinea(@PathVariable int id, @PathVariable int linea) {
        return Dtos.PedidoDto.de(gestor.quitarLinea(id, linea));
    }

    @PostMapping("/pedidos/{id}/avanzar")
    public Dtos.PedidoDto avanzar(@PathVariable int id) {
        return Dtos.PedidoDto.de(gestor.avanzarEstado(id));
    }

    @PostMapping("/pedidos/{id}/cancelar")
    public Dtos.PedidoDto cancelar(@PathVariable int id) {
        return Dtos.PedidoDto.de(gestor.cancelar(id));
    }
}
