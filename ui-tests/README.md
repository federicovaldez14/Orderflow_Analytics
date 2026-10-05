# Pruebas de UI (bonificación) — Selenium

Las pruebas viven junto al resto de pruebas Java para reutilizar Maven, Spring Boot Test y la base
H2 de pruebas:

```
src/test/java/com/restaurant/uitests/
├── PosWebUiIT.java          4 pruebas (3 flujos principales + 1 mejora de UX verificada)
├── Navegador.java           crea Edge / Chrome / Firefox (Selenium Manager descarga el driver)
└── paginas/                 Page Object Model
    ├── PaginaBase.java      esperas explícitas (WebDriverWait), clic tolerante al repintado
    ├── PaginaSalon.java     mapa de mesas y panel de la mesa
    ├── DialogoDivision.java diálogo "Dividir la cuenta"
    ├── PaginaCocina.java    tablero de comandas
    └── PaginaInventario.java
```

| Prueba | Flujo |
|---|---|
| `shouldTakeOrderAndDeliverItThroughKitchen` | Mesero toma la comanda de la mesa 3 → cocina la lleva de *Creado* a *Entregado* → la mesa queda libre |
| `shouldSplitBillByConsumptionAndMatchTotal` | Dividir la cuenta por consumo con un plato compartido y 10 % de propina → la suma de las partes da exacto (R2) |
| `shouldShowLowStockAlertAfterPhysicalCount` | Conteo físico del aguacate → alerta de stock bajo en la fila y en el menú (R1) |
| `shouldKeepUnsentOrderWhenSwitchingTables` | Mejora de UX H5-1: cambiar de mesa no borra la comanda sin enviar |

**Cómo correrlas** (levantan la aplicación sola en un puerto aleatorio):

```bash
mvn verify -Pui-tests                          # todas las pruebas + UI (Edge en Windows, Chrome en Linux/macOS)
mvn verify -Pui-tests -Dui.navegador=chrome    # otro navegador: chrome | edge | firefox
mvn verify -Pui-tests -Dui.visible=true        # ver el navegador mientras corren
```

En GitHub Actions corren en el trabajo `pruebas-ui` (`.github/workflows/ci.yml`) con Chrome sin
ventana.

**Buenas prácticas aplicadas:** Page Object Model (las pruebas no tienen selectores), esperas
explícitas (sin `Thread.sleep`), selectores por atributos de datos (`data-mesa`, `data-plato`,
`data-accion`, `data-estado`) y no por posición, base de datos limpia y página recargada antes de
cada prueba (no dependen del orden).
