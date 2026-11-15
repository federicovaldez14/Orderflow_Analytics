package com.restaurant.infraestructura.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * GET /api/sesion: quién está conectado y con qué rol (la web lo usa para
 * decidir si muestra el login y qué pestañas muestra). El login y el logout
 * los atiende Spring Security en /api/sesion/login y /api/sesion/logout.
 */
@RestController
public class SesionController {

    public record SesionDto(String usuario, List<String> roles) {
    }

    @GetMapping("/api/sesion")
    public ResponseEntity<?> sesion(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", 401, "error", "Unauthorized", "mensaje", "Inicie sesión"));
        }
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .toList();
        return ResponseEntity.ok(new SesionDto(auth.getName(), roles));
    }
}
