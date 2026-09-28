package com.restaurant.infraestructura.persistencia;

import com.restaurant.aplicacion.puerto.salida.MenuRepositorio;
import com.restaurant.dominio.fabrica.TipoPlato;
import com.restaurant.dominio.modelo.Plato;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Adaptador de salida: la carta del restaurante en una tabla (H2). */
public class MenuRepositorioJdbc implements MenuRepositorio {

    private final DataSource dataSource;

    public MenuRepositorioJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initSchema() {
        ejecutar("CREATE TABLE IF NOT EXISTS plato(" +
                " nombre VARCHAR(80) PRIMARY KEY," +
                " tipo VARCHAR(20) NOT NULL," +
                " precio BIGINT NOT NULL," +
                " tiempo_prep INT NOT NULL)");
    }

    @Override
    public List<Plato> listar() {
        String sql = "SELECT nombre, tipo, precio, tiempo_prep FROM plato ORDER BY tipo, nombre";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Plato> platos = new ArrayList<>();
            while (rs.next()) {
                platos.add(mapear(rs));
            }
            return platos;
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No se pudo leer la carta", e);
        }
    }

    @Override
    public Optional<Plato> buscarPorNombre(String nombre) {
        String sql = "SELECT nombre, tipo, precio, tiempo_prep FROM plato WHERE nombre = ?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No se pudo buscar el plato " + nombre, e);
        }
    }

    @Override
    public void guardar(Plato plato) {
        String update = "UPDATE plato SET tipo = ?, precio = ?, tiempo_prep = ? WHERE nombre = ?";
        String insert = "INSERT INTO plato(tipo, precio, tiempo_prep, nombre) VALUES(?, ?, ?, ?)";
        try (Connection con = dataSource.getConnection()) {
            int filas;
            try (PreparedStatement ps = con.prepareStatement(update)) {
                llenar(ps, plato);
                filas = ps.executeUpdate();
            }
            if (filas == 0) {
                try (PreparedStatement ps = con.prepareStatement(insert)) {
                    llenar(ps, plato);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No se pudo guardar el plato " + plato.getNombre(), e);
        }
    }

    private static void llenar(PreparedStatement ps, Plato plato) throws SQLException {
        ps.setString(1, plato.getTipo().name());
        ps.setLong(2, plato.getPrecio());
        ps.setInt(3, plato.getTiempoPreparacionMinutos());
        ps.setString(4, plato.getNombre());
    }

    private static Plato mapear(ResultSet rs) throws SQLException {
        return new Plato(rs.getString("nombre"), TipoPlato.valueOf(rs.getString("tipo")),
                rs.getLong("precio"), rs.getInt("tiempo_prep"));
    }

    private void ejecutar(String ddl) {
        try (Connection con = dataSource.getConnection(); Statement st = con.createStatement()) {
            st.execute(ddl);
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No se pudo crear el esquema de la carta", e);
        }
    }
}
