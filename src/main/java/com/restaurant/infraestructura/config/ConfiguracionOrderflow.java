package com.restaurant.infraestructura.config;

import com.restaurant.aplicacion.casodeuso.GestorPedidos;
import com.restaurant.aplicacion.casodeuso.ServicioAnalitica;
import com.restaurant.aplicacion.casodeuso.ServicioCuenta;
import com.restaurant.aplicacion.casodeuso.ServicioInventario;
import com.restaurant.aplicacion.puerto.salida.AlertaInventario;
import com.restaurant.aplicacion.puerto.salida.InventarioRepositorio;
import com.restaurant.aplicacion.puerto.salida.MenuRepositorio;
import com.restaurant.aplicacion.puerto.salida.PedidoRepositorio;
import com.restaurant.dominio.observador.Notificador;
import com.restaurant.infraestructura.notificacion.AlertasInventarioEnMemoria;
import com.restaurant.infraestructura.notificacion.NotificadorCocina;
import com.restaurant.infraestructura.notificacion.NotificadorMesero;
import com.restaurant.infraestructura.persistencia.InventarioRepositorioJdbc;
import com.restaurant.infraestructura.persistencia.MenuRepositorioJdbc;
import com.restaurant.infraestructura.persistencia.PedidoRepositorioJdbc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.List;

/**
 * Composition root de la arquitectura hexagonal: el ÚNICO lugar donde se
 * decide qué adaptador concreto implementa cada puerto. El dominio y la capa
 * de aplicación no tienen anotaciones de Spring; se crean aquí con new.
 */
@Configuration
public class ConfiguracionOrderflow {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public MenuRepositorio menuRepositorio(DataSource dataSource) {
        MenuRepositorioJdbc repo = new MenuRepositorioJdbc(dataSource);
        repo.initSchema();
        return repo;
    }

    @Bean
    public PedidoRepositorio pedidoRepositorio(DataSource dataSource, Clock reloj) {
        PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(dataSource, reloj);
        repo.initSchema();
        return repo;
    }

    @Bean
    public NotificadorCocina notificadorCocina() {
        return new NotificadorCocina();
    }

    @Bean
    public NotificadorMesero notificadorMesero() {
        return new NotificadorMesero();
    }

    @Bean
    public InventarioRepositorio inventarioRepositorio(DataSource dataSource) {
        InventarioRepositorioJdbc repo = new InventarioRepositorioJdbc(dataSource);
        repo.initSchema();
        return repo;
    }

    @Bean
    public AlertasInventarioEnMemoria alertasInventario() {
        return new AlertasInventarioEnMemoria();
    }

    @Bean
    public ServicioInventario servicioInventario(InventarioRepositorio repo, List<AlertaInventario> alertas,
                                                 Clock reloj) {
        return new ServicioInventario(repo, alertas, reloj);
    }

    @Bean
    public GestorPedidos gestorPedidos(PedidoRepositorio pedidos, MenuRepositorio menu,
                                       ServicioInventario inventario, List<Notificador> notificadores,
                                       Clock reloj, @Value("${orderflow.mesas:10}") int numeroMesas) {
        return new GestorPedidos(pedidos, menu, inventario, notificadores, numeroMesas, reloj);
    }

    @Bean
    public ServicioCuenta servicioCuenta(PedidoRepositorio pedidos) {
        return new ServicioCuenta(pedidos);
    }

    @Bean
    public ServicioAnalitica servicioAnalitica(PedidoRepositorio pedidos, Clock reloj) {
        return new ServicioAnalitica(pedidos, reloj);
    }
}
