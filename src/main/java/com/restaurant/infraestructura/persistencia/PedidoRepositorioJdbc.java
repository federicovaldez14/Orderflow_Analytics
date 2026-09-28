package com.restaurant.infraestructura.persistencia;

import com.restaurant.aplicacion.puerto.salida.PedidoRepositorio;
import com.restaurant.dominio.fabrica.TipoPlato;
import com.restaurant.dominio.modelo.ItemPedido;
import com.restaurant.dominio.modelo.Pedido;
import com.restaurant.dominio.modelo.Plato;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador de salida: guarda pedidos en H2 con JDBC plano (como el
 * RegistryRepository de los talleres), pero sobre un DataSource con pool
 * (HikariCP) en vez de DriverManager: en el taller de carga abrir una
 * conexión por operación fue el cuello de botella (defecto PERF-01).
 *
 * Cada línea guarda una "foto" del plato (nombre, tipo, precio): si mañana
 * sube el precio en la carta, los pedidos ya cobrados no cambian.
 */
public class PedidoRepositorioJdbc implements PedidoRepositorio {

    private static final String COLUMNAS_PEDIDO = "id, mesa, estado, hora_creacion, hora_en_preparacion, "
            + "hora_listo, hora_entregado, hora_cancelado";

    private final DataSource dataSource;
    private final Clock reloj;

    public PedidoRepositorioJdbc(DataSource dataSource, Clock reloj) {
        this.dataSource = dataSource;
        this.reloj = reloj;
    }

    public void initSchema() {
        try (Connection con = dataSource.getConnection(); Statement st = con.createStatement()) {
            st.execute("CREATE SEQUENCE IF NOT EXISTS seq_pedido START WITH 1");
            st.execute("CREATE TABLE IF NOT EXISTS pedido(" +
                    " id INT PRIMARY KEY," +
                    " mesa INT NOT NULL," +
                    " estado VARCHAR(30) NOT NULL," +
                    // 'activo' lo decide el dominio (Pedido.estaEditable) al guardar;
                    // la consulta solo filtra, no repite la regla de negocio.
                    " activo BOOLEAN NOT NULL," +
                    " hora_creacion TIMESTAMP NOT NULL," +
                    " hora_en_preparacion TIMESTAMP," +
                    " hora_listo TIMESTAMP," +
                    " hora_entregado TIMESTAMP," +
                    " hora_cancelado TIMESTAMP)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_pedido_mesa_activo ON pedido(mesa, activo)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_pedido_creacion ON pedido(hora_creacion)");
            st.execute("CREATE TABLE IF NOT EXISTS item_pedido(" +
                    " pedido_id INT NOT NULL," +
                    " linea INT NOT NULL," +
                    " plato VARCHAR(80) NOT NULL," +
                    " tipo VARCHAR(20) NOT NULL," +
                    " precio BIGINT NOT NULL," +
                    " tiempo_prep INT NOT NULL," +
                    " cantidad INT NOT NULL," +
                    " PRIMARY KEY (pedido_id, linea)," +
                    " FOREIGN KEY (pedido_id) REFERENCES pedido(id))");
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No se pudo crear el esquema de pedidos", e);
        }
    }

    @Override
    public int siguienteId() {
        try (Connection con = dataSource.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT NEXT VALUE FOR seq_pedido")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No se pudo reservar un id de pedido", e);
        }
    }

    /** Cabecera + líneas en UNA transacción: o se guarda todo o nada. */
    @Override
    public void guardar(Pedido pedido) {
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (actualizarCabecera(con, pedido) == 0) {
                    insertarCabecera(con, pedido);
                }
                reemplazarLineas(con, pedido);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No se pudo guardar el pedido #" + pedido.getId(), e);
        }
    }

    @Override
    public Optional<Pedido> buscarPorId(int id) {
        List<Pedido> r = consultar("SELECT " + COLUMNAS_PEDIDO + " FROM pedido WHERE id = ?", id);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    @Override
    public Optional<Pedido> buscarActivoPorMesa(int mesa) {
        List<Pedido> r = consultar("SELECT " + COLUMNAS_PEDIDO
                + " FROM pedido WHERE mesa = ? AND activo = TRUE ORDER BY id DESC", mesa);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    @Override
    public List<Pedido> listarTodos() {
        return consultar("SELECT " + COLUMNAS_PEDIDO + " FROM pedido ORDER BY id");
    }

    @Override
    public List<Pedido> listarCreadosDesde(LocalDateTime desde) {
        return consultar("SELECT " + COLUMNAS_PEDIDO + " FROM pedido WHERE hora_creacion >= ? ORDER BY id", desde);
    }

    /** Solo para pruebas: deja las tablas vacías. */
    public void eliminarTodos() {
        try (Connection con = dataSource.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM item_pedido");
            st.executeUpdate("DELETE FROM pedido");
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No se pudieron borrar los pedidos", e);
        }
    }

    // ------------------------------------------------------------------

    private int actualizarCabecera(Connection con, Pedido p) throws SQLException {
        String sql = "UPDATE pedido SET mesa = ?, estado = ?, activo = ?, hora_creacion = ?, "
                + "hora_en_preparacion = ?, hora_listo = ?, hora_entregado = ?, hora_cancelado = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            llenarCabecera(ps, p);
            return ps.executeUpdate();
        }
    }

    private void insertarCabecera(Connection con, Pedido p) throws SQLException {
        String sql = "INSERT INTO pedido(mesa, estado, activo, hora_creacion, hora_en_preparacion, "
                + "hora_listo, hora_entregado, hora_cancelado, id) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            llenarCabecera(ps, p);
            ps.executeUpdate();
        }
    }

    private static void llenarCabecera(PreparedStatement ps, Pedido p) throws SQLException {
        ps.setInt(1, p.getMesa());
        ps.setString(2, p.getEstadoNombre());
        ps.setBoolean(3, p.estaEditable());
        ps.setObject(4, p.getHoraCreacion());
        fecha(ps, 5, p.getHoraEnPreparacion());
        fecha(ps, 6, p.getHoraListo());
        fecha(ps, 7, p.getHoraEntregado());
        fecha(ps, 8, p.getHoraCancelado());
        ps.setInt(9, p.getId());
    }

    private static void fecha(PreparedStatement ps, int i, LocalDateTime valor) throws SQLException {
        if (valor == null) {
            ps.setNull(i, Types.TIMESTAMP);
        } else {
            ps.setObject(i, valor);
        }
    }

    private static void reemplazarLineas(Connection con, Pedido p) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM item_pedido WHERE pedido_id = ?")) {
            del.setInt(1, p.getId());
            del.executeUpdate();
        }
        String sql = "INSERT INTO item_pedido(pedido_id, linea, plato, tipo, precio, tiempo_prep, cantidad) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ins = con.prepareStatement(sql)) {
            int linea = 0;
            for (ItemPedido item : p.getItems()) {
                Plato plato = item.getPlato();
                ins.setInt(1, p.getId());
                ins.setInt(2, linea++);
                ins.setString(3, plato.getNombre());
                ins.setString(4, plato.getTipo().name());
                ins.setLong(5, plato.getPrecio());
                ins.setInt(6, plato.getTiempoPreparacionMinutos());
                ins.setInt(7, item.getCantidad());
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }

    /**
     * Lee cabeceras y luego TODAS sus líneas en una segunda consulta (no una
     * por pedido): evita el problema N+1, que bajo carga multiplica las idas
     * a la base de datos.
     */
    private List<Pedido> consultar(String sql, Object... params) {
        try (Connection con = dataSource.getConnection()) {
            Map<Integer, Object[]> cabeceras = new LinkedHashMap<>();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        cabeceras.put(rs.getInt("id"), new Object[]{
                                rs.getInt("mesa"), rs.getString("estado"),
                                rs.getObject("hora_creacion", LocalDateTime.class),
                                rs.getObject("hora_en_preparacion", LocalDateTime.class),
                                rs.getObject("hora_listo", LocalDateTime.class),
                                rs.getObject("hora_entregado", LocalDateTime.class),
                                rs.getObject("hora_cancelado", LocalDateTime.class)});
                    }
                }
            }
            if (cabeceras.isEmpty()) {
                return new ArrayList<>();
            }
            Map<Integer, List<ItemPedido>> lineas = leerLineas(con, cabeceras.keySet());
            List<Pedido> resultado = new ArrayList<>();
            for (Map.Entry<Integer, Object[]> e : cabeceras.entrySet()) {
                Object[] c = e.getValue();
                resultado.add(Pedido.reconstituir(e.getKey(), (Integer) c[0], (String) c[1],
                        lineas.getOrDefault(e.getKey(), List.of()),
                        (LocalDateTime) c[2], (LocalDateTime) c[3], (LocalDateTime) c[4],
                        (LocalDateTime) c[5], (LocalDateTime) c[6], reloj));
            }
            return resultado;
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No se pudieron leer los pedidos", e);
        }
    }

    private static Map<Integer, List<ItemPedido>> leerLineas(Connection con, java.util.Set<Integer> ids)
            throws SQLException {
        Map<Integer, List<ItemPedido>> lineas = new LinkedHashMap<>();
        // Pocos pedidos: IN (?, ?, ...). Muchos (p. ej. listar todo para la
        // analítica): una sola lectura completa, filtrada en memoria.
        boolean enLista = ids.size() <= 500;
        String sql = enLista
                ? "SELECT * FROM item_pedido WHERE pedido_id IN ("
                    + String.join(",", java.util.Collections.nCopies(ids.size(), "?")) + ") ORDER BY pedido_id, linea"
                : "SELECT * FROM item_pedido ORDER BY pedido_id, linea";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (enLista) {
                int i = 1;
                for (Integer id : ids) {
                    ps.setInt(i++, id);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int pedidoId = rs.getInt("pedido_id");
                    if (!ids.contains(pedidoId)) {
                        continue;
                    }
                    Plato plato = new Plato(rs.getString("plato"), TipoPlato.valueOf(rs.getString("tipo")),
                            rs.getLong("precio"), rs.getInt("tiempo_prep"));
                    lineas.computeIfAbsent(pedidoId, k -> new ArrayList<>())
                            .add(new ItemPedido(plato, rs.getInt("cantidad")));
                }
            }
        }
        return lineas;
    }
}
