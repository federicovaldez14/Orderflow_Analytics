package com.restaurant.infraestructura.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Mitigación de repudio (STRIDE): toda petición que cambia datos queda en el
 * log con el usuario autenticado, el método, la ruta y el código de respuesta.
 * Así "yo no ajusté ese inventario" se puede comprobar en los logs.
 */
class FiltroAuditoria extends OncePerRequestFilter {

    private static final Logger AUDITORIA = LoggerFactory.getLogger("AUDITORIA");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(req, res);
        } finally {
            if (cambiaDatos(req.getMethod()) && req.getRequestURI().startsWith("/api/")) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String usuario = auth == null || !auth.isAuthenticated() ? "anonimo" : auth.getName();
                AUDITORIA.info("usuario={} {} {} -> {}", usuario, req.getMethod(),
                        req.getRequestURI().replaceAll("[\r\n]", "_"), res.getStatus());
            }
        }
    }

    private static boolean cambiaDatos(String metodo) {
        return "POST".equals(metodo) || "PUT".equals(metodo) || "PATCH".equals(metodo) || "DELETE".equals(metodo);
    }
}
