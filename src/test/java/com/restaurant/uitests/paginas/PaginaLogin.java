package com.restaurant.uitests.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.stream.Collectors;

/** Page Object de la pantalla de inicio de sesión y del menú según el rol. */
public class PaginaLogin extends PaginaBase {

    private static final By PANTALLA = By.id("pantalla-login");

    public PaginaLogin(WebDriver driver, String urlBase) {
        super(driver, urlBase);
    }

    /** Inicia sesión y espera a que desaparezca la pantalla de login. */
    public void entrarComo(String usuario, String clave) {
        visible(PANTALLA);
        escribir(By.id("login-usuario"), usuario);
        escribir(By.id("login-clave"), clave);
        clic(By.id("login-entrar"));
        espera.until(ExpectedConditions.invisibilityOfElementLocated(PANTALLA));
    }

    /** Intenta entrar con datos equivocados: la pantalla de login sigue visible y muestra el error. */
    public void entrarConError(String usuario, String clave) {
        visible(PANTALLA);
        escribir(By.id("login-usuario"), usuario);
        escribir(By.id("login-clave"), clave);
        clic(By.id("login-entrar"));
        visible(By.id("login-error"));
    }

    public String errorMostrado() {
        return visible(By.id("login-error")).getText();
    }

    /** Secciones del menú que ve el usuario conectado. */
    public List<String> seccionesVisibles() {
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("usuario-actual")));
        return driver.findElements(By.cssSelector("#nav a")).stream()
                .filter(a -> a.isDisplayed())
                .map(a -> a.getAttribute("data-vista"))
                .collect(Collectors.toList());
    }

    public void cerrarSesion() {
        clic(By.id("cerrar-sesion"));
        visible(PANTALLA);
    }
}
