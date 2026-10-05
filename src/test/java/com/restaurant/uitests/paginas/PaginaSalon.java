package com.restaurant.uitests.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Page Object de la vista Salón: mapa de mesas y panel de la mesa seleccionada. */
public class PaginaSalon extends PaginaBase {

    private static final By TITULO_PANEL = By.cssSelector("#panel-mesa .titulo-mesa");

    public PaginaSalon(WebDriver driver, String urlBase) {
        super(driver, urlBase);
    }

    public PaginaSalon abrir() {
        abrirVista("salon", "Salón");
        visible(By.cssSelector("button.mesa[data-mesa]"));
        return this;
    }

    public PaginaSalon seleccionarMesa(int mesa) {
        clic(mesa(mesa));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(TITULO_PANEL, String.format("Mesa %02d", mesa)));
        return this;
    }

    /** Toca el plato en la carta del panel tantas veces como unidades se quieran. */
    public PaginaSalon agregarPlato(String plato, int unidades) {
        By boton = By.cssSelector("#panel-mesa .plato[data-plato=\"" + plato + "\"]");
        for (int i = 0; i < unidades; i++) {
            clic(boton);
        }
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("#panel-mesa .ticket"), plato));
        return this;
    }

    public PaginaSalon enviarACocina() {
        clic(By.cssSelector("#panel-mesa [data-accion='confirmar']"));
        return this;
    }

    public PaginaSalon esperarEstadoMesa(int mesa, String estado) {
        espera.until(ExpectedConditions.attributeToBe(mesa(mesa), "data-estado", estado));
        return this;
    }

    /** La mesa muestra en el mapa que tiene una comanda sin enviar. */
    public PaginaSalon esperarBorradorEnMesa(int mesa) {
        visible(By.cssSelector("button.mesa[data-mesa='" + mesa + "'] [data-borrador]"));
        return this;
    }

    /** Unidades de un plato en la comanda que se está armando (0 si no está). */
    public int unidadesEnComanda(String plato) {
        return driver.findElements(By.xpath("//*[@id='panel-mesa']//button[@data-sumar=\"" + plato
                        + "\"]/preceding-sibling::span[1]")).stream()
                .findFirst().map(e -> Integer.parseInt(e.getText().trim())).orElse(0);
    }

    public String totalDelPanel() {
        return normalizar(visible(By.cssSelector("#panel-mesa .total-linea b")).getText());
    }

    public DialogoDivision abrirDivisionDeCuenta() {
        clic(By.cssSelector("#panel-mesa [data-accion='dividir']"));
        return new DialogoDivision(driver, espera);
    }

    private static By mesa(int mesa) {
        return By.cssSelector("button.mesa[data-mesa='" + mesa + "']");
    }
}
