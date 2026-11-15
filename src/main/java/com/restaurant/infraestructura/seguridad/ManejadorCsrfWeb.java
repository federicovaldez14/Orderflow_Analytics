package com.restaurant.infraestructura.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Manejo del token CSRF para una web de una sola página (receta oficial de
 * Spring Security 6): la web lee la cookie XSRF-TOKEN y devuelve el valor en la
 * cabecera X-XSRF-TOKEN. Se carga el token en cada petición para que la cookie
 * exista antes del primer POST (Spring Security 6 lo carga de forma diferida).
 */
final class ManejadorCsrfWeb implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plano = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, Supplier<CsrfToken> token) {
        xor.handle(req, res, token);
        token.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest req, CsrfToken token) {
        String cabecera = req.getHeader(token.getHeaderName());
        return (StringUtils.hasText(cabecera) ? plano : xor).resolveCsrfTokenValue(req, token);
    }
}
