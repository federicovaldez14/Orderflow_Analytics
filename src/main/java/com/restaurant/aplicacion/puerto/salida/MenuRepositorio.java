package com.restaurant.aplicacion.puerto.salida;

import com.restaurant.dominio.modelo.Plato;

import java.util.List;
import java.util.Optional;

/** Puerto de salida para consultar la carta del restaurante. */
public interface MenuRepositorio {

    List<Plato> listar();

    Optional<Plato> buscarPorNombre(String nombre);

    void guardar(Plato plato);
}
