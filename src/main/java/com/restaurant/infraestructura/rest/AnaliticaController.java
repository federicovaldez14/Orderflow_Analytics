package com.restaurant.infraestructura.rest;

import com.restaurant.aplicacion.casodeuso.ServicioAnalitica;
import com.restaurant.dominio.reportes.Periodo;
import com.restaurant.dominio.reportes.Reporte;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador REST del Reto 3. Entrega los mismos datos que grafica la UI, en
 * JSON, para que cualquier otro cliente (un tablero web, Excel, k6) los use.
 *
 *   GET /api/analitica?periodo=HOY|SEMANA|TODO
 *   GET /api/analitica/reportes/{id}?periodo=...
 */
@RestController
@RequestMapping("/api/analitica")
public class AnaliticaController {

    private final ServicioAnalitica analitica;

    public AnaliticaController(ServicioAnalitica analitica) {
        this.analitica = analitica;
    }

    @GetMapping
    public ServicioAnalitica.PanelAnalitico panel(@RequestParam(value = "periodo", defaultValue = "TODO") String periodo) {
        return analitica.panel(periodo(periodo));
    }

    @GetMapping("/reportes/{id}")
    public Reporte reporte(@PathVariable String id,
                           @RequestParam(value = "periodo", defaultValue = "TODO") String periodo) {
        return analitica.reporte(id, periodo(periodo));
    }

    private static Periodo periodo(String texto) {
        try {
            return Periodo.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Periodo inválido '" + texto + "' (usa HOY, SEMANA o TODO)");
        }
    }
}
