package com.restaurant.infraestructura.rest;

import com.restaurant.aplicacion.excepcion.RecursoNoEncontradoException;
import com.restaurant.aplicacion.excepcion.ReglaNegocioException;
import com.restaurant.dominio.inventario.StockInsuficienteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del núcleo a códigos HTTP. Gracias a esto el
 * dominio lanza excepciones normales de Java y no sabe nada de HTTP.
 *
 *   IllegalArgumentException / JSON mal formado -> 400 (dato inválido)
 *   RecursoNoEncontradoException               -> 404
 *   ReglaNegocioException / IllegalStateException -> 409 (el negocio no lo permite ahora)
 *   StockInsuficienteException                 -> 409 con el detalle de lo que falta
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Dtos.ErrorDto> datoInvalido(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Dtos.ErrorDto> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({ReglaNegocioException.class, IllegalStateException.class})
    public ResponseEntity<Dtos.ErrorDto> conflicto(RuntimeException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Dtos.ErrorStockDto> sinStock(StockInsuficienteException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Dtos.ErrorStockDto(
                HttpStatus.CONFLICT.value(), "Stock insuficiente", e.getMessage(), e.getFaltantes()));
    }

    private static ResponseEntity<Dtos.ErrorDto> respuesta(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status)
                .body(new Dtos.ErrorDto(status.value(), status.getReasonPhrase(), mensaje));
    }
}
