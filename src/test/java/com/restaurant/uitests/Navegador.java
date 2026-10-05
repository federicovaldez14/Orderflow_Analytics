package com.restaurant.uitests;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Crea el navegador de las pruebas de UI. Selenium Manager descarga el driver
 * que corresponda, así que no hay que instalar nada a mano.
 *
 *   -Dui.navegador=chrome|edge|firefox   (por defecto: Edge en Windows, Chrome en el resto)
 *   -Dui.visible=true                    para ver el navegador (por defecto corre sin ventana)
 */
final class Navegador {

    private Navegador() {
    }

    static WebDriver crear() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String nombre = System.getProperty("ui.navegador", windows ? "edge" : "chrome");
        boolean sinVentana = !Boolean.getBoolean("ui.visible");
        String tamano = "--window-size=1440,900";   // escritorio: salón con su panel lateral

        switch (nombre) {
            case "edge": {
                EdgeOptions o = new EdgeOptions();
                o.addArguments(tamano);
                if (sinVentana) {
                    o.addArguments("--headless=new");
                }
                return new EdgeDriver(o);
            }
            case "firefox": {
                FirefoxOptions o = new FirefoxOptions();
                o.addArguments("--width=1440", "--height=900");
                if (sinVentana) {
                    o.addArguments("-headless");
                }
                return new FirefoxDriver(o);
            }
            default: {
                ChromeOptions o = new ChromeOptions();
                o.addArguments(tamano, "--no-sandbox", "--disable-dev-shm-usage");
                if (sinVentana) {
                    o.addArguments("--headless=new");
                }
                return new ChromeDriver(o);
            }
        }
    }
}
