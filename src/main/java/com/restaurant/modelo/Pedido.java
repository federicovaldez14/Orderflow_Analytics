package com.restaurant.modelo;

import com.restaurant.estado.EstadoCreado;
import com.restaurant.estado.EstadoPedido;
import com.restaurant.observador.Notificador;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pedido es el "contexto" del patrón State y el "sujeto" del patrón Observer.
 *
 * SRP: esta clase solo conoce sus propios datos y cómo delegar el cambio
 * de estado; NO decide cómo se crean los platos (eso es de PlatoFactory)
 * ni cómo se generan reportes (eso es de ReporteService).
 *
 * DIP: depende de las abstracciones EstadoPedido y Notificador, nunca de
 * una implementación concreta como EstadoListo o NotificadorCocina.
 */
public class Pedido {

    private final int id;
    private final int mesa;
    private final List<ItemPedido> items = new ArrayList<>();
    private final List<Notificador> observadores = new ArrayList<>();

    private EstadoPedido estadoActual;
    private final LocalDateTime horaCreacion;
    private LocalDateTime horaListo;
    private LocalDateTime horaEntregado;

    public Pedido(int id, int mesa) {
        this.id = id;
        this.mesa = mesa;
        this.estadoActual = new EstadoCreado();
        this.horaCreacion = LocalDateTime.now();
    }

    public void agregarItem(ItemPedido item) {
        items.add(item);
    }

    public void agregarObservador(Notificador observador) {
        observadores.add(observador);
    }

    /** Pide al estado actual que avance el flujo (delegación del patrón State). */
    public void avanzarEstado() {
        estadoActual.avanzar(this);
    }

    /**
     * Usado por las clases EstadoXxx para hacer la transición real.
     * Aquí es donde se registran los timestamps para medir tiempos de
     * atención y donde se notifica a los observadores (Observer).
     */
    public void cambiarEstado(EstadoPedido nuevoEstado) {
        this.estadoActual = nuevoEstado;

        if (nuevoEstado.getNombre().equals("Listo")) {
            horaListo = LocalDateTime.now();
        } else if (nuevoEstado.getNombre().equals("Entregado")) {
            horaEntregado = LocalDateTime.now();
        }

        notificarObservadores("cambió a estado '" + nuevoEstado.getNombre() + "'");
    }

    private void notificarObservadores(String mensaje) {
        for (Notificador observador : observadores) {
            observador.notificar(this, mensaje);
        }
    }

    public double calcularTotal() {
        double total = 0;
        for (ItemPedido item : items) {
            total += item.subtotal();
        }
        return total;
    }

    /** Tiempo entre creación y entrega; null si aún no se ha entregado. */
    public Duration tiempoDeAtencion() {
        if (horaEntregado == null) {
            return null;
        }
        return Duration.between(horaCreacion, horaEntregado);
    }

    public int getId() {
        return id;
    }

    public int getMesa() {
        return mesa;
    }

    public List<ItemPedido> getItems() {
        return Collections.unmodifiableList(items);
    }

    public String getEstadoNombre() {
        return estadoActual.getNombre();
    }

    public LocalDateTime getHoraCreacion() {
        return horaCreacion;
    }
}
