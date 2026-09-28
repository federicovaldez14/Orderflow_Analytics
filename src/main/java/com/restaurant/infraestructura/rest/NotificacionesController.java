package com.restaurant.infraestructura.rest;

import com.restaurant.infraestructura.notificacion.AlertasInventarioEnMemoria;
import com.restaurant.infraestructura.notificacion.NotificadorCocina;
import com.restaurant.infraestructura.notificacion.NotificadorMesero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone por HTTP las mismas notificaciones que muestra la pestaña
 * "Notificaciones" del POS Swing (cocina, mesero y alertas de stock), para
 * que la versión web las pueda mostrar. Solo lee los buffers acotados en
 * memoria; no hay reglas de negocio aquí.
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionesController {

    public record NotificacionesDto(List<String> cocina, List<String> mesero, List<String> inventario) {
    }

    private final NotificadorCocina cocina;
    private final NotificadorMesero mesero;
    private final AlertasInventarioEnMemoria alertas;

    public NotificacionesController(NotificadorCocina cocina, NotificadorMesero mesero,
                                    AlertasInventarioEnMemoria alertas) {
        this.cocina = cocina;
        this.mesero = mesero;
        this.alertas = alertas;
    }

    @GetMapping
    public NotificacionesDto recientes() {
        return new NotificacionesDto(cocina.recientes(), mesero.recientes(), alertas.recientes());
    }
}
