package com.restaurant.aplicacion.casodeuso;

import com.restaurant.aplicacion.excepcion.RecursoNoEncontradoException;
import com.restaurant.aplicacion.excepcion.ReglaNegocioException;
import com.restaurant.aplicacion.puerto.salida.MenuRepositorio;
import com.restaurant.aplicacion.puerto.salida.PedidoRepositorio;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.modelo.Plato;
import com.restaurant.dominio.observador.Notificador;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Caso de uso: administrar el ciclo de vida de los pedidos.
 *
 * Es la misma clase del Corte 1, ahora como servicio de la capa de aplicación
 * de la arquitectura hexagonal. Recibe todo lo externo por puertos
 * (PedidoRepositorio, MenuRepositorio, Notificador) y lo usan por igual la
 * UI Swing y la API REST: los dos adaptadores de entrada llaman a los mismos
 * métodos, por eso las reglas no se duplican.
 *
 * Concurrencia: varios meseros (o varias peticiones HTTP) pueden tocar el
 * mismo pedido o la misma mesa a la vez. Cada operación toma un candado por
 * pedido o por mesa para que dos cambios simultáneos no se pisen.
 * Limitación: el candado vive en memoria, así que solo protege dentro de UNA
 * instancia de la aplicación (ver "límites conocidos" en docs/arquitectura.md).
 */
public class GestorPedidos {

    private final PedidoRepositorio pedidos;
    private final MenuRepositorio menu;
    private final ControlInventario inventario;
    private final List<Notificador> notificadores;
    private final int numeroMesas;
    private final Clock reloj;
    private final ConcurrentHashMap<String, ReentrantLock> candados = new ConcurrentHashMap<>();

    public GestorPedidos(PedidoRepositorio pedidos, MenuRepositorio menu, ControlInventario inventario,
                         List<Notificador> notificadores, int numeroMesas, Clock reloj) {
        if (numeroMesas <= 0) {
            throw new IllegalArgumentException("El restaurante debe tener al menos una mesa");
        }
        this.pedidos = pedidos;
        this.menu = menu;
        this.inventario = inventario;
        this.notificadores = List.copyOf(notificadores);
        this.numeroMesas = numeroMesas;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    /**
     * Abre un pedido para una mesa con sus platos.
     * Reglas: la mesa existe, está libre y el pedido trae al menos un plato
     * que esté en la carta.
     */
    public Pedido crearPedido(int mesa, List<LineaSolicitada> lineas) {
        validarMesa(mesa);
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un plato");
        }
        return conCandado("mesa-" + mesa, () -> {
            Optional<Pedido> activo = pedidos.buscarActivoPorMesa(mesa);
            if (activo.isPresent()) {
                throw new ReglaNegocioException("La mesa " + mesa + " ya tiene el pedido activo #"
                        + activo.get().getId() + "; agrega los platos a ese pedido.");
            }
            Pedido pedido = new Pedido(pedidos.siguienteId(), mesa, reloj);
            for (LineaSolicitada linea : lineas) {
                pedido.agregarItem(new ItemPedido(platoDeLaCarta(linea.plato()), linea.cantidad()));
            }
            // Reto 1: si no alcanza el inventario, el pedido no se crea.
            inventario.reservar(pedido, pedido.getItems());
            guardarOCompensar(pedido, pedido.getItems());
            conObservadores(pedido);
            notificarCreacion(pedido);
            return pedido;
        });
    }

    public Pedido agregarItem(int pedidoId, LineaSolicitada linea) {
        return conCandado("pedido-" + pedidoId, () -> {
            Pedido pedido = cargar(pedidoId);
            ItemPedido item = new ItemPedido(platoDeLaCarta(linea.plato()), linea.cantidad());
            pedido.agregarItem(item);
            inventario.reservar(pedido, List.of(item));
            guardarOCompensar(pedido, List.of(item));
            return pedido;
        });
    }

    public Pedido quitarLinea(int pedidoId, int indiceLinea) {
        return conCandado("pedido-" + pedidoId, () -> {
            Pedido pedido = cargar(pedidoId);
            String estadoAntes = pedido.getEstadoNombre();
            ItemPedido quitado = pedido.removerLinea(indiceLinea);
            pedidos.guardar(pedido);
            inventario.liberar(pedido, List.of(quitado), estadoAntes);
            return pedido;
        });
    }

    public Pedido avanzarEstado(int pedidoId) {
        return conCandado("pedido-" + pedidoId, () -> {
            Pedido pedido = cargar(pedidoId);
            if (!pedido.estaEditable()) {
                throw new ReglaNegocioException("El pedido #" + pedidoId + " ya está '"
                        + pedido.getEstadoNombre() + "'; no puede avanzar más.");
            }
            pedido.avanzarEstado();
            pedidos.guardar(pedido);
            return pedido;
        });
    }

    public Pedido cancelar(int pedidoId) {
        return conCandado("pedido-" + pedidoId, () -> {
            Pedido pedido = cargar(pedidoId);
            String estadoAntes = pedido.getEstadoNombre();
            try {
                pedido.cancelar();
            } catch (IllegalStateException e) {
                throw new ReglaNegocioException(e.getMessage());
            }
            pedidos.guardar(pedido);
            inventario.liberar(pedido, pedido.getItems(), estadoAntes);
            return pedido;
        });
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public Pedido obtener(int pedidoId) {
        return pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido #" + pedidoId));
    }

    public List<Pedido> listar() {
        return pedidos.listarTodos();
    }

    public Optional<Pedido> pedidoActivoDeMesa(int mesa) {
        return pedidos.buscarActivoPorMesa(mesa);
    }

    public List<Plato> menu() {
        return menu.listar();
    }

    public int getNumeroMesas() {
        return numeroMesas;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /**
     * El inventario ya se descontó; si guardar el pedido falla, se devuelve
     * lo descontado para no dejar ingredientes "vendidos" en un pedido que no
     * existe (compensación manual: el inventario y los pedidos no comparten
     * una transacción, ver límites en docs/arquitectura.md).
     */
    private void guardarOCompensar(Pedido pedido, List<ItemPedido> reservados) {
        try {
            pedidos.guardar(pedido);
        } catch (RuntimeException e) {
            inventario.liberar(pedido, reservados, "Creado");
            throw e;
        }
    }

    private Pedido cargar(int pedidoId) {
        return conObservadores(obtener(pedidoId));
    }

    private Pedido conObservadores(Pedido pedido) {
        for (Notificador n : notificadores) {
            pedido.agregarObservador(n);
        }
        return pedido;
    }

    private void notificarCreacion(Pedido pedido) {
        for (Notificador n : notificadores) {
            n.notificar(pedido, "nuevo pedido con " + pedido.getItems().size() + " línea(s)");
        }
    }

    private Plato platoDeLaCarta(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del plato es obligatorio");
        }
        return menu.buscarPorNombre(nombre.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("El plato '" + nombre + "' no está en la carta"));
    }

    private void validarMesa(int mesa) {
        if (mesa < 1 || mesa > numeroMesas) {
            throw new IllegalArgumentException("La mesa debe estar entre 1 y " + numeroMesas + ": " + mesa);
        }
    }

    private <T> T conCandado(String clave, Supplier<T> accion) {
        ReentrantLock candado = candados.computeIfAbsent(clave, k -> new ReentrantLock());
        candado.lock();
        try {
            return accion.get();
        } finally {
            candado.unlock();
        }
    }
}
