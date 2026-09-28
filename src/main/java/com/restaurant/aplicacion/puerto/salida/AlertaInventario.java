package com.restaurant.aplicacion.puerto.salida;

import com.restaurant.dominio.inventario.Ingrediente;

/**
 * Puerto de salida (Observer): a quién avisar cuando un ingrediente baja del
 * mínimo. Hoy lo implementa un canal en memoria que muestra la UI; mañana
 * podría ser un correo al proveedor sin tocar el caso de uso.
 */
public interface AlertaInventario {
    void stockBajo(Ingrediente ingrediente);
}
