package com.restaurant.uitests.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Page Object de la vista Inventario (Reto 1). */
public class PaginaInventario extends PaginaBase {

    public PaginaInventario(WebDriver driver, String urlBase) {
        super(driver, urlBase);
    }

    public PaginaInventario abrir() {
        abrirVista("inventario", "Inventario");
        visible(By.cssSelector("tr[data-codigo]"));
        return this;
    }

    /** Registra un conteo físico desde el diálogo "Ajustar". */
    public PaginaInventario ajustarPorConteo(String codigo, long stockContado) {
        clic(By.cssSelector("tr[data-codigo='" + codigo + "'] [data-ajustar]"));
        escribir(By.cssSelector("dialog[open] #cant"), String.valueOf(stockContado));
        clic(By.cssSelector("dialog[open] button[type='submit']"));
        espera.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("dialog[open]")));
        return this;
    }

    public PaginaInventario esperarAlertaBajoMinimo(String codigo) {
        visible(By.cssSelector("tr[data-codigo='" + codigo + "'] .alerta-stock"));
        return this;
    }

    public String stockMostrado(String codigo) {
        return normalizar(visible(By.cssSelector("tr[data-codigo='" + codigo + "'] td.num")).getText());
    }

    public String insigniaDeAlertas() {
        return visible(By.id("insignia-inventario")).getText();
    }
}
