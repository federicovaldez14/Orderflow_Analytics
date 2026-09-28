package com.restaurant.infraestructura.notificacion;

import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.observador.Notificador;
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

/**
 * Adaptador base del puerto Notificador (Observer).
 *
 * Corte 1 imprimía con System.out. En Corte 2 las notificaciones llegan
 * desde varios hilos (peticiones HTTP concurrentes y la UI), así que:
 * - se registran con un logger (thread-safe y con nivel configurable),
 * - se guardan las últimas N en memoria para que la pantalla las muestre,
 * - y se avisa a quien se haya suscrito (la pestaña "Notificaciones").
 * El buffer es acotado a propósito: bajo carga llegan miles de mensajes y
 * una lista sin límite acabaría con la memoria.
 */
public abstract class NotificadorEnMemoria implements Notificador {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAXIMO = 300;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String etiqueta;
    private final Deque<String> recientes = new ArrayDeque<>();
    private final List<Consumer<String>> suscriptores = new CopyOnWriteArrayList<>();

    protected NotificadorEnMemoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public void notificar(Pedido pedido, String mensaje) {
        if (!leInteresa(mensaje)) {
            return;
        }
        String linea = String.format("[%s] %s | Pedido #%d (mesa %d): %s",
                LocalTime.now().format(HORA), etiqueta, pedido.getId(), pedido.getMesa(), mensaje);
        log.debug(linea);
        synchronized (recientes) {
            recientes.addLast(linea);
            while (recientes.size() > MAXIMO) {
                recientes.removeFirst();
            }
        }
        for (Consumer<String> s : suscriptores) {
            s.accept(linea);
        }
    }

    /** Cada canal decide qué eventos le importan (por defecto, todos). */
    protected boolean leInteresa(String mensaje) {
        return true;
    }

    public void suscribir(Consumer<String> suscriptor) {
        suscriptores.add(suscriptor);
    }

    public List<String> recientes() {
        synchronized (recientes) {
            return new ArrayList<>(recientes);
        }
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
