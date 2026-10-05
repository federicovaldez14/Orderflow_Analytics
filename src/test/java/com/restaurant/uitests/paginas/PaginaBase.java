package com.restaurant.uitests.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Base de los Page Objects del POS web.
 *
 * Solo hay esperas explícitas (WebDriverWait + ExpectedConditions), nunca
 * Thread.sleep. La web se repinta sola cada pocos segundos, así que un
 * elemento puede reemplazarse entre que se encuentra y se usa: por eso las
 * esperas ignoran StaleElementReference y vuelven a buscarlo. Los selectores
 * usan atributos de datos (data-mesa, data-plato, data-accion...), no la
 * posición de los elementos.
 */
public abstract class PaginaBase {

    protected final WebDriver driver;
    protected final WebDriverWait espera;
    private final String urlBase;

    protected PaginaBase(WebDriver driver, String urlBase) {
        this.driver = driver;
        this.urlBase = urlBase;
        this.espera = new WebDriverWait(driver, Duration.ofSeconds(10));
        this.espera.ignoring(StaleElementReferenceException.class)
                .ignoring(ElementClickInterceptedException.class);
    }

    /** Navega a una vista (#/salon, #/cocina...) y espera a que aparezca su título. */
    protected void abrirVista(String vista, String titulo) {
        driver.get(urlBase + "/#/" + vista);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".encabezado h1"), titulo));
    }

    protected WebElement visible(By selector) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(selector));
    }

    /** Clic tolerante al repintado: si el elemento cambió o algo lo tapa un instante, lo vuelve a buscar. */
    protected void clic(By selector) {
        espera.until(d -> {
            WebElement e = d.findElement(selector);
            if (!e.isDisplayed() || !e.isEnabled()) {
                return false;
            }
            e.click();
            return true;
        });
    }

    /** Reemplaza el valor de un campo disparando los eventos de teclado (como un usuario). */
    protected void escribir(By selector, String valor) {
        WebElement campo = visible(selector);
        campo.sendKeys(Keys.chord(Keys.CONTROL, "a"), valor);
    }

    /** Espera el aviso flotante (toast) que contiene el texto y lo devuelve. */
    public String esperarAviso(String texto) {
        return visible(By.xpath("//div[contains(@class,'toast')][contains(normalize-space(.), \"" + texto + "\")]"))
                .getText();
    }

    protected static String normalizar(String texto) {
        return texto.replace(' ', ' ').replaceAll("\\s+", " ").trim();
    }
}
