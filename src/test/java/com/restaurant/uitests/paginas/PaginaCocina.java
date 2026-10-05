package com.restaurant.uitests.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Page Object de la vista Cocina: tablero de comandas por estado. */
public class PaginaCocina extends PaginaBase {

    public PaginaCocina(WebDriver driver, String urlBase) {
        super(driver, urlBase);
    }

    public PaginaCocina abrir() {
        abrirVista("cocina", "Cocina");
        return this;
    }

    public PaginaCocina esperarComanda(int mesa, String estado) {
        visible(By.cssSelector("article.comanda[data-mesa='" + mesa + "'][data-estado='" + estado + "']"));
        return this;
    }

    /** Pulsa el botón principal de la comanda (Iniciar preparación / Marcar como listo / Entregar). */
    public PaginaCocina avanzar(int mesa) {
        clic(By.cssSelector("article.comanda[data-mesa='" + mesa + "'] [data-avanzar]"));
        return this;
    }

    public PaginaCocina esperarSinComanda(int mesa) {
        espera.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("article.comanda[data-mesa='" + mesa + "']")));
        return this;
    }
}
