package com.restaurant;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Punto de entrada único. Levanta:
 * - la API REST (adaptador de entrada HTTP) en el puerto 8080,
 * - y, si orderflow.ui.enabled=true y hay pantalla, el POS de escritorio
 *   (adaptador de entrada Swing) conectado a los MISMOS casos de uso.
 *
 * headless(false): Spring Boot arranca por defecto en modo sin pantalla,
 * lo que impediría abrir la ventana Swing.
 */
@SpringBootApplication
public class OrderflowApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(OrderflowApplication.class)
                .headless(false)
                .run(args);
    }
}
