# Guía de pruebas manuales

Esta guía te lleva paso a paso para que **tú mismo** compruebes cada parte
del sistema. Cada prueba te dice: qué código escribir, dónde escribirlo,
cómo correrlo, y qué deberías ver si todo funciona bien.

**Recomendación:** no edites `Main.java` directamente (ya está en el
entregable). Crea un archivo nuevo, por ejemplo
`src/main/java/com/restaurant/MisPruebas.java`, y ve agregando código ahí
prueba por prueba. Así siempre puedes volver a comparar contra el
`Main.java` original.

Antes de cada prueba, compila con:

```bash
javac -d out $(find src -name "*.java")
```

Y corre tu archivo con:

```bash
java -Dfile.encoding=UTF-8 -cp out com.restaurant.MisPruebas
```

---

## Prueba 1 — Factory Method: cada tipo de plato tiene su propio tiempo de preparación

**Qué verifica:** que `PlatoFactory` asigna reglas distintas según la
categoría, sin que tú tengas que especificarlas manualmente.

```java
package com.restaurant;

import com.restaurant.fabrica.PlatoFactory;
import com.restaurant.fabrica.TipoPlato;
import com.restaurant.modelo.Plato;

public class MisPruebas {
    public static void main(String[] args) {
        Plato entrada = PlatoFactory.crear(TipoPlato.ENTRADA, "Empanadas", 6000);
        Plato fuerte  = PlatoFactory.crear(TipoPlato.FUERTE, "Sancocho", 25000);
        Plato bebida  = PlatoFactory.crear(TipoPlato.BEBIDA, "Jugo de mora", 7000);
        Plato postre  = PlatoFactory.crear(TipoPlato.POSTRE, "Arroz con leche", 6000);

        System.out.println(entrada + " -> " + entrada.getTiempoPreparacionMinutos() + " min");
        System.out.println(fuerte  + " -> " + fuerte.getTiempoPreparacionMinutos() + " min");
        System.out.println(bebida  + " -> " + bebida.getTiempoPreparacionMinutos() + " min");
        System.out.println(postre  + " -> " + postre.getTiempoPreparacionMinutos() + " min");
    }
}
```

**Deberías ver:** 8 min para entrada, 18 min para fuerte, 2 min para
bebida, 6 min para postre — esos números vienen de `PlatoFactory.java`,
no los pusiste tú al crear el plato. Ábrelo y confirma que coinciden.

---

## Prueba 2 — State: el flujo de estados avanza en orden y no se puede saltar pasos

**Qué verifica:** que un pedido nuevo siempre empieza en `Creado` y que
cada llamada a `avanzarEstado()` mueve exactamente un paso.

```java
Pedido pedido = new Pedido(99, 10); // id=99, mesa=10 (puedes construirlo directo para esta prueba)
System.out.println("Estado inicial: " + pedido.getEstadoNombre());

pedido.avanzarEstado();
System.out.println("Después de 1 avance: " + pedido.getEstadoNombre());

pedido.avanzarEstado();
System.out.println("Después de 2 avances: " + pedido.getEstadoNombre());

pedido.avanzarEstado();
System.out.println("Después de 3 avances: " + pedido.getEstadoNombre());
```

**Deberías ver:** `Creado` → `En preparación` → `Listo` → `Entregado`,
uno por línea, en ese orden exacto. Si intercambias el orden de las
llamadas no puedes "saltar" a Entregado sin pasar por los intermedios.

---

## Prueba 3 — Caso borde de State: un pedido ya entregado no debe romperse ni retroceder

**Qué verifica:** que `EstadoEntregado.avanzar()` es un estado terminal
seguro (no lanza excepción, no cambia nada).

```java
pedido.avanzarEstado(); // pedido ya estaba en "Entregado" desde la prueba 2
System.out.println("Después de avanzar un pedido ya entregado: " + pedido.getEstadoNombre());
```

**Deberías ver:** sigue diciendo `Entregado`. Si el programa lanzara una
excepción o el estado cambiara a algo raro, ahí habría un bug.

---

## Prueba 4 — Observer: agregar o quitar observadores cambia quién se entera

**Qué verifica:** que `Pedido` no sabe (ni le importa) qué observadores
tiene conectados — el mismo cambio de estado puede notificar a 0, 1 o 2
canales según lo que tú conectes.

```java
Pedido pedidoA = new Pedido(1, 4);
pedidoA.agregarObservador(new com.restaurant.observador.NotificadorCocina());
// Nota: aquí NO agregamos NotificadorMesero a propósito

System.out.println("--- pedidoA (solo cocina conectada) ---");
pedidoA.avanzarEstado();

Pedido pedidoB = new Pedido(2, 6);
pedidoB.agregarObservador(new com.restaurant.observador.NotificadorCocina());
pedidoB.agregarObservador(new com.restaurant.observador.NotificadorMesero());

System.out.println("--- pedidoB (cocina y mesero conectados) ---");
pedidoB.avanzarEstado();
```

**Deberías ver:** para `pedidoA` solo aparece una línea `[COCINA] ...`;
para `pedidoB` aparecen dos líneas, `[COCINA] ...` y `[MESERO] ...`. Esto
comprueba que `Pedido` no tiene ninguna referencia "quemada" a cocina o
mesero: solo notifica a la lista que tú le diste.

---

## Prueba 5 — Extensibilidad real del Observer (sin tocar ninguna clase existente)

**Qué verifica:** el punto más importante del diseño — que puedes agregar
un canal de notificación **nuevo** sin modificar `Pedido`, `EstadoPedido`
ni ningún archivo ya entregado. Esto es la prueba de que el
Open/Closed Principle no es solo un comentario en el código, sino algo
que realmente puedes hacer.

Crea un archivo nuevo `src/main/java/com/restaurant/MiNotificadorLog.java`:

```java
package com.restaurant;

import com.restaurant.modelo.Pedido;
import com.restaurant.observador.Notificador;

/** Un canal de notificación inventado por ti, sin tocar nada existente. */
public class MiNotificadorLog implements Notificador {
    @Override
    public void notificar(Pedido pedido, String mensaje) {
        System.out.println(">> LOG PERSONALIZADO: pedido " + pedido.getId() + " -> " + mensaje);
    }
}
```

Y en `MisPruebas.java`:

```java
Pedido pedidoC = new Pedido(3, 7);
pedidoC.agregarObservador(new MiNotificadorLog());
pedidoC.avanzarEstado();
```

**Deberías ver:** la línea `>> LOG PERSONALIZADO: ...` sin haber editado
`Pedido.java` ni `Notificador.java` — solo creaste una clase nueva que
implementa la interfaz. Si esto compila y funciona, el diseño realmente
está "abierto a extensión, cerrado a modificación".

---

## Prueba 6 — Cálculo de totales con distintas cantidades

**Qué verifica:** que `ItemPedido.subtotal()` y `Pedido.calcularTotal()`
suman bien cuando hay varias líneas con cantidades distintas.

```java
Plato cafe = PlatoFactory.crear(TipoPlato.BEBIDA, "Café", 3000);
Pedido pedidoD = new Pedido(4, 2);
pedidoD.agregarItem(new ItemPedido(cafe, 5));   // 5 x 3000 = 15000
pedidoD.agregarItem(new ItemPedido(bebida, 1)); // usa el "bebida" de la Prueba 1
System.out.println("Total esperado: 15000 + " + bebida.getPrecio());
System.out.println("Total calculado: " + pedidoD.calcularTotal());
```

**Deberías ver:** que "Total esperado" y "Total calculado" coinciden
exactamente. Cambia las cantidades y verifica a mano con calculadora.

---

## Prueba 7 — Strategy: el mismo conjunto de pedidos, tres reportes distintos

**Qué verifica:** que puedes generar reportes diferentes sin cambiar cómo
armaste los pedidos — solo cambia qué `EstrategiaReporte` le pasas a
`ReporteService`.

```java
List<Pedido> pedidos = Arrays.asList(pedidoA, pedidoB, pedidoD);
ReporteService servicio = new ReporteService();

System.out.println(servicio.generarReporte(new ReportePlatosMasPedidos(), pedidos));
System.out.println(servicio.generarReporte(new ReportePlatosMenosPedidos(), pedidos));
System.out.println(servicio.generarReporte(new ReporteTiempoPromedio(), pedidos));
```

**Deberías ver:** tres bloques de texto distintos generados a partir de
la misma lista `pedidos`. El de "más pedidos" y "menos pedidos" deben
mostrar el mismo conteo pero en orden invertido.

---

## Prueba 8 — Caso borde de Strategy: reporte de tiempo con pedidos no entregados

**Qué verifica:** que el reporte de tiempo promedio no cuenta pedidos que
no han llegado a "Entregado" (y no se cae con `NullPointerException`).

```java
Pedido pedidoE = new Pedido(5, 9); // se queda en "Creado", nunca se entrega
System.out.println(servicio.generarReporte(new ReporteTiempoPromedio(), Arrays.asList(pedidoE)));
```

**Deberías ver:** el mensaje `"Aún no hay pedidos entregados."` en vez de
un error o un promedio de 0 pedidos "fantasma".

---

## Prueba 9 — Extensibilidad del State (crear un estado nuevo tú mismo)

**Qué verifica:** que puedes extender el flujo de estados sin tocar
`Pedido.java` — la misma idea de la Prueba 5, pero aplicada al patrón
State en vez de Observer.

Crea `src/main/java/com/restaurant/MiEstadoCancelado.java`:

```java
package com.restaurant;

import com.restaurant.estado.EstadoPedido;
import com.restaurant.modelo.Pedido;

public class MiEstadoCancelado implements EstadoPedido {
    @Override
    public String getNombre() {
        return "Cancelado";
    }
    @Override
    public void avanzar(Pedido pedido) {
        // estado terminal también: cancelado no avanza a ningún lado
    }
}
```

Y en `MisPruebas.java`:

```java
Pedido pedidoF = new Pedido(6, 12);
pedidoF.cambiarEstado(new MiEstadoCancelado());
System.out.println("Estado de pedidoF: " + pedidoF.getEstadoNombre());
pedidoF.avanzarEstado(); // no debería pasar nada
System.out.println("Estado de pedidoF después de avanzar: " + pedidoF.getEstadoNombre());
```

**Deberías ver:** `Cancelado` las dos veces, y en consola verás también
las notificaciones `[COCINA]`/`[MESERO]` avisando "cambió a estado
'Cancelado'" — sin haber tocado ni una línea de `Pedido.java` ni de
`EstadoPedido.java`.

---

## Prueba 10 — Todo junto: compara contra `Main.java` y `PruebaCompleta.java`

Por último, corre los dos archivos que ya vienen en el proyecto y
compara mentalmente contra lo que fuiste viendo en las pruebas 1-9:

```bash
java -Dfile.encoding=UTF-8 -cp out com.restaurant.Main
java -Dfile.encoding=UTF-8 -cp out com.restaurant.prueba.PruebaCompleta
```

Si entendiste por qué cada línea de esa salida aparece (qué la produjo y
en qué archivo vive esa lógica), ya puedes explicar tu propio código en
la sustentación sin depender de este documento.

## Checklist rápido

- [ ] Prueba 1 — Factory Method asigna tiempos distintos por categoría
- [ ] Prueba 2 — State avanza en orden Creado→...→Entregado
- [ ] Prueba 3 — Estado Entregado es terminal (no se rompe ni retrocede)
- [ ] Prueba 4 — Observer notifica solo a quien está conectado
- [ ] Prueba 5 — Se puede agregar un observador nuevo sin tocar código existente
- [ ] Prueba 6 — Los totales suman correctamente
- [ ] Prueba 7 — Strategy genera reportes distintos con los mismos datos
- [ ] Prueba 8 — El reporte de tiempo ignora pedidos no entregados
- [ ] Prueba 9 — Se puede agregar un estado nuevo sin tocar código existente
- [ ] Prueba 10 — `Main.java` y `PruebaCompleta.java` corren sin errores
