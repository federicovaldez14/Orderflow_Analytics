package com.restaurant.infraestructura.notificacion;

import com.restaurant.aplicacion.puerto.salida.AlertaInventario;
import com.restaurant.dominio.inventario.Ingrediente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Adaptador del puerto AlertaInventario: registra la alerta y avisa a la pantalla. */
public class AlertasInventarioEnMemoria implements AlertaInventario {

    private static final Logger LOG = LoggerFactory.getLogger(AlertasInventarioEnMemoria.class);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAXIMO = 100;

    private final Deque<String> recientes = new ArrayDeque<>();
    private final List<Consumer<String>> suscriptores = new CopyOnWriteArrayList<>();

    @Override
    public void stockBajo(Ingrediente ing) {
        String linea = String.format("[%s] STOCK BAJO: %s quedan %d %s (mínimo %d)", LocalTime.now().format(HORA),
                ing.getNombre(), ing.getStock(), ing.getUnidad().getSimbolo(), ing.getStockMinimo());
        LOG.warn(linea);
        synchronized (recientes) {
            recientes.addLast(linea);
            while (recientes.size() > MAXIMO) {
                recientes.removeFirst();
            }
        }
        suscriptores.forEach(s -> s.accept(linea));
    }

    public void suscribir(Consumer<String> suscriptor) {
        suscriptores.add(suscriptor);
    }

    public List<String> recientes() {
        synchronized (recientes) {
            return new ArrayList<>(recientes);
        }
    }
}
