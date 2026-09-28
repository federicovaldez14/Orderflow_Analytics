package com.restaurant.aplicacion.casodeuso;

/** Lo que pide el mesero para una línea: nombre del plato en la carta y cantidad. */
public record LineaSolicitada(String plato, int cantidad) {
}
