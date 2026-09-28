package com.restaurant.infraestructura.rest;

import com.restaurant.aplicacion.casodeuso.ServicioCuenta;
import com.restaurant.dominio.cuenta.DivisionIgualitaria;
import com.restaurant.dominio.cuenta.DivisionPorConsumo;
import com.restaurant.dominio.cuenta.DivisionPorPorcentaje;
import com.restaurant.dominio.cuenta.EstrategiaDivision;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adaptador REST del Reto 2. Su único trabajo propio es traducir el JSON a
 * la estrategia correspondiente (Strategy); el cálculo lo hace el dominio.
 */
@RestController
@RequestMapping("/api/pedidos")
public class DivisionCuentaController {

    private final ServicioCuenta servicio;

    public DivisionCuentaController(ServicioCuenta servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/{id}/division")
    public Dtos.DivisionDto dividir(@PathVariable int id, @RequestBody Dtos.SolicitudDivisionDto solicitud) {
        int propina = solicitud.propinaPorcentaje() == null ? 0 : solicitud.propinaPorcentaje();
        return Dtos.DivisionDto.de(servicio.dividir(id, estrategia(solicitud), propina));
    }

    static EstrategiaDivision estrategia(Dtos.SolicitudDivisionDto s) {
        String metodo = s.metodo() == null ? "" : s.metodo().trim().toUpperCase();
        switch (metodo) {
            case "IGUALITARIA":
                if (s.personas() != null && !s.personas().isEmpty()) {
                    return new DivisionIgualitaria(s.personas());
                }
                if (s.numeroPersonas() == null) {
                    throw new IllegalArgumentException("Indica 'personas' o 'numeroPersonas'");
                }
                return DivisionIgualitaria.entre(s.numeroPersonas());
            case "POR_CONSUMO":
                if (s.asignaciones() == null) {
                    throw new IllegalArgumentException("Indica 'asignaciones' (línea -> personas)");
                }
                Map<Integer, Map<String, Integer>> asignaciones = new LinkedHashMap<>();
                for (Dtos.AsignacionDto a : s.asignaciones()) {
                    if (asignaciones.put(a.linea(), a.personas()) != null) {
                        throw new IllegalArgumentException("La línea " + a.linea() + " está asignada dos veces");
                    }
                }
                return new DivisionPorConsumo(asignaciones);
            case "POR_PORCENTAJE":
                return new DivisionPorPorcentaje(s.porcentajes());
            default:
                throw new IllegalArgumentException(
                        "Método de división desconocido: '" + s.metodo() + "' (usa IGUALITARIA, POR_CONSUMO o POR_PORCENTAJE)");
        }
    }
}
