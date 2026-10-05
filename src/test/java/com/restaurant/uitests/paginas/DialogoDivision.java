package com.restaurant.uitests.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.stream.Collectors;

/** Page Object del diálogo "Dividir la cuenta" (Reto 2). */
public class DialogoDivision {

    private static final By DIALOGO = By.cssSelector("dialog[open]");
    private final WebDriverWait espera;
    private final WebDriver driver;

    DialogoDivision(WebDriver driver, WebDriverWait espera) {
        this.driver = driver;
        this.espera = espera;
        espera.until(ExpectedConditions.visibilityOfElementLocated(DIALOGO));
    }

    /** IGUALITARIA, POR_CONSUMO o POR_PORCENTAJE. */
    public DialogoDivision elegirMetodo(String metodo) {
        clic(By.cssSelector("dialog[open] [data-metodo='" + metodo + "']"));
        return this;
    }

    public DialogoDivision nombrarPersona(int indice, String nombre) {
        escribir(By.cssSelector("dialog[open] [data-nombre='" + indice + "']"), nombre);
        return this;
    }

    /** Cuántas partes de la línea {@code linea} le tocan a la persona {@code persona} (0 = no consumió). */
    public DialogoDivision partes(int linea, int persona, int partes) {
        escribir(By.cssSelector("dialog[open] [data-peso='" + linea + "-" + persona + "']"), String.valueOf(partes));
        return this;
    }

    public DialogoDivision propina(int porcentaje) {
        clic(By.cssSelector("dialog[open] [data-propina='" + porcentaje + "']"));
        return this;
    }

    /** Pide la división al servidor y devuelve el texto de verificación ("Total ... = suma de las partes ..."). */
    public String calcular() {
        clic(By.cssSelector("dialog[open] #calcular"));
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("dialog[open] .verificacion")))
                .getText().replace(' ', ' ');
    }

    /** Texto de cada tarjeta de persona del resultado. */
    public List<String> partesDelResultado() {
        return driver.findElements(By.cssSelector("dialog[open] .parte")).stream()
                .map(WebElement::getText)
                .map(t -> t.replace(' ', ' '))
                .collect(Collectors.toList());
    }

    private void clic(By selector) {
        espera.ignoring(StaleElementReferenceException.class).ignoring(ElementClickInterceptedException.class)
                .until(d -> {
                    WebElement e = d.findElement(selector);
                    if (!e.isDisplayed() || !e.isEnabled()) {
                        return false;
                    }
                    e.click();
                    return true;
                });
    }

    private void escribir(By selector, String valor) {
        espera.until(ExpectedConditions.visibilityOfElementLocated(selector))
                .sendKeys(Keys.chord(Keys.CONTROL, "a"), valor);
    }
}
