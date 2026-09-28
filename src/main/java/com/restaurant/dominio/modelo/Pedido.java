package com.restaurant.dominio.modelo;

import com.restaurant.dominio.estado.EstadoCancelado;
import com.restaurant.dominio.estado.EstadoCreado;
import com.restaurant.dominio.estado.EstadoPedido;
import com.restaurant.dominio.estado.EstadosPedido;
import com.restaurant.dominio.observador.Notificador;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pedido es el "contexto" del patrón State y el "sujeto" del patrón Observer.
 *
 * SRP: solo conoce sus datos y cómo delegar el cambio de estado.
 * DIP: depende de las abstracciones EstadoPedido y Notificador.
 *
 * Cambios de Corte 2:
 * - Las reglas que antes vivían en la UI (no quitar el último plato, no
 *   cancelar un pedido terminado) se movieron aquí: en hexagonal las reglas
 *   de negocio pertenecen al núcleo, no a un adaptador.
 * - Recibe un {@link Clock}: las pruebas controlan el tiempo sin Thread.sleep.
 * - {@link #reconstituir} permite al adaptador de persistencia volver a
 *   armar un pedido desde la base de datos sin disparar notificaciones.
 */
public class Pedido {

    private final int id;
    private final int mesa;
    private final List<ItemPedido> items = new ArrayList<>();
    private final List<Notificador> observadores = new ArrayList<>();
    private final Clock reloj;

    private EstadoPedido estadoActual;
    private final LocalDateTime horaCreacion;
    private LocalDateTime horaEnPreparacion;
    private LocalDateTime horaListo;
    private LocalDateTime horaEntregado;
    private LocalDateTime horaCancelado;

    public Pedido(int id, int mesa) {
        this(id, mesa, Clock.systemDefaultZone());
    }

    public Pedido(int id, int mesa, Clock reloj) {
        if (id <= 0) {
            throw new IllegalArgumentException("El id del pedido debe ser positivo: " + id);
        }
        if (mesa <= 0) {
            throw new IllegalArgumentException("El número de mesa debe ser positivo: " + mesa);
        }
        this.id = id;
        this.mesa = mesa;
        this.reloj = reloj;
        this.estadoActual = new EstadoCreado();
        this.horaCreacion = LocalDateTime.now(reloj);
    }

    private Pedido(int id, int mesa, Clock reloj, LocalDateTime horaCreacion) {
        this.id = id;
        this.mesa = mesa;
        this.reloj = reloj;
        this.horaCreacion = horaCreacion;
    }

    /**
     * Reconstruye un pedido tal como estaba guardado. No valida transiciones
     * ni notifica: el pedido ya existía, solo se está leyendo.
     */
    public static Pedido reconstituir(int id, int mesa, String estado, List<ItemPedido> items,
                                      LocalDateTime horaCreacion, LocalDateTime horaEnPreparacion,
                                      LocalDateTime horaListo, LocalDateTime horaEntregado,
                                      LocalDateTime horaCancelado, Clock reloj) {
        Pedido p = new Pedido(id, mesa, reloj, horaCreacion);
        p.estadoActual = EstadosPedido.desdeNombre(estado);
        p.items.addAll(items);
        p.horaEnPreparacion = horaEnPreparacion;
        p.horaListo = horaListo;
        p.horaEntregado = horaEntregado;
        p.horaCancelado = horaCancelado;
        return p;
    }

    // ------------------------------------------------------------------
    // Ítems
    // ------------------------------------------------------------------

    public void agregarItem(ItemPedido item) {
        exigirEditable();
        if (item == null) {
            throw new IllegalArgumentException("El ítem es obligatorio");
        }
        items.add(item);
    }

    /**
     * Quita la línea en la posición indicada y la devuelve (el caso de uso la
     * necesita para devolver ingredientes al inventario).
     * Regla: un pedido no puede quedar vacío; para eso existe cancelar().
     */
    public ItemPedido removerLinea(int indice) {
        exigirEditable();
        if (indice < 0 || indice >= items.size()) {
            throw new IllegalArgumentException("No existe la línea " + indice + " en el pedido #" + id);
        }
        if (items.size() == 1) {
            throw new IllegalStateException(
                    "El pedido #" + id + " debe tener al menos un plato; si quieres vaciarlo, cancélalo.");
        }
        return items.remove(indice);
    }

    /** true si el pedido todavía admite cambios en sus ítems. */
    public boolean estaEditable() {
        return !estadoActual.esTerminal();
    }

    private void exigirEditable() {
        if (!estaEditable()) {
            throw new IllegalStateException(
                    "No se puede modificar el pedido #" + id + ": ya está en estado terminal ('"
                            + estadoActual.getNombre() + "').");
        }
    }

    // ------------------------------------------------------------------
    // Estados (State) y notificaciones (Observer)
    // ------------------------------------------------------------------

    public void agregarObservador(Notificador observador) {
        observadores.add(observador);
    }

    /** Pide al estado actual que avance el flujo (delegación del patrón State). */
    public void avanzarEstado() {
        estadoActual.avanzar(this);
    }

    /** Cancela el pedido. Regla: solo si todavía no está Entregado ni Cancelado. */
    public void cancelar() {
        if (estadoActual.esTerminal()) {
            throw new IllegalStateException(
                    "El pedido #" + id + " ya está '" + estadoActual.getNombre() + "'; no se puede cancelar.");
        }
        cambiarEstado(new EstadoCancelado());
    }

    /**
     * Usado por las clases EstadoXxx para hacer la transición real. Aquí se
     * registran los timestamps (base de la analítica de tiempos) y se
     * notifica a los observadores.
     */
    public void cambiarEstado(EstadoPedido nuevoEstado) {
        this.estadoActual = nuevoEstado;
        LocalDateTime ahora = LocalDateTime.now(reloj);
        switch (nuevoEstado.getNombre()) {
            case "En preparación":
                horaEnPreparacion = ahora;
                break;
            case "Listo":
                horaListo = ahora;
                break;
            case "Entregado":
                horaEntregado = ahora;
                break;
            case "Cancelado":
                horaCancelado = ahora;
                break;
            default:
                break;
        }
        notificarObservadores("cambió a estado '" + nuevoEstado.getNombre() + "'");
    }

    private void notificarObservadores(String mensaje) {
        for (Notificador observador : observadores) {
            observador.notificar(this, mensaje);
        }
    }

    // ------------------------------------------------------------------
    // Cálculos
    // ------------------------------------------------------------------

    public long calcularTotal() {
        long total = 0;
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

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

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

    public LocalDateTime getHoraEnPreparacion() {
        return horaEnPreparacion;
    }

    public LocalDateTime getHoraListo() {
        return horaListo;
    }

    public LocalDateTime getHoraEntregado() {
        return horaEntregado;
    }

    public LocalDateTime getHoraCancelado() {
        return horaCancelado;
    }
}
