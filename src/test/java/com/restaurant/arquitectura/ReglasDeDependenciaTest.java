package com.restaurant.arquitectura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de las REGLAS DEL ESTILO (hexagonal): las dependencias solo apuntan
 * hacia adentro.
 *
 *   dominio      -> no conoce aplicación, infraestructura, Spring, JDBC ni Swing
 *   aplicación   -> no conoce infraestructura, Spring, JDBC ni Swing
 *
 * Lee los import de los .java. Si alguien, por comodidad, importa un
 * adaptador desde el núcleo, esta prueba falla y el build se pone en rojo:
 * es la evidencia de que "el código respeta el estilo elegido".
 */
class ReglasDeDependenciaTest {

    private static final Path FUENTES = Paths.get("src", "main", "java", "com", "restaurant");

    @Test
    @DisplayName("El dominio no depende de aplicación, infraestructura ni frameworks")
    void dominioNoDependeDeNadieExterno() throws IOException {
        List<String> violaciones = importsProhibidos(FUENTES.resolve("dominio"),
                "com.restaurant.aplicacion", "com.restaurant.infraestructura",
                "org.springframework", "java.sql", "javax.sql", "javax.swing", "java.awt");
        assertTrue(violaciones.isEmpty(), "Dependencias prohibidas en dominio: " + violaciones);
    }

    @Test
    @DisplayName("La capa de aplicación no depende de infraestructura ni frameworks")
    void aplicacionNoDependeDeInfraestructura() throws IOException {
        List<String> violaciones = importsProhibidos(FUENTES.resolve("aplicacion"),
                "com.restaurant.infraestructura", "org.springframework",
                "java.sql", "javax.sql", "javax.swing", "java.awt");
        assertTrue(violaciones.isEmpty(), "Dependencias prohibidas en aplicación: " + violaciones);
    }

    @Test
    @DisplayName("El núcleo existe (la prueba no pasa en vacío por una ruta equivocada)")
    void elNucleoTieneClases() throws IOException {
        assertTrue(archivos(FUENTES.resolve("dominio")).size() > 5);
        assertTrue(archivos(FUENTES.resolve("aplicacion")).size() > 3);
    }

    private static List<String> importsProhibidos(Path carpeta, String... prefijos) throws IOException {
        List<String> violaciones = new ArrayList<>();
        for (Path archivo : archivos(carpeta)) {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                String l = linea.trim();
                if (!l.startsWith("import ")) {
                    continue;
                }
                for (String prefijo : prefijos) {
                    if (l.startsWith("import " + prefijo) || l.startsWith("import static " + prefijo)) {
                        violaciones.add(archivo.getFileName() + " -> " + l);
                    }
                }
            }
        }
        return violaciones;
    }

    private static List<Path> archivos(Path carpeta) throws IOException {
        try (Stream<Path> s = Files.walk(carpeta)) {
            return s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }
}
