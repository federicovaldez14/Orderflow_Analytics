# Guía técnica — Compilación y ejecución

Este proyecto usa **Java puro** (sin Maven/Gradle ni frameworks) a
propósito, para mantenerse dentro del alcance de los Módulos 1 y 2:
fundamentos, SOLID y patrones de diseño, sin añadir complejidad de
build tools o dependencias externas.

## Requisitos
- JDK 11 o superior instalado (`javac -version` para verificar).

## Compilar

Desde la raíz del proyecto:

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
```

## Ejecutar la interfaz gráfica (POS) — recomendado para probar el sistema

```bash
java -cp out com.restaurant.ui.PosApp
```

Esto abre una ventana con 4 pestañas:

- **Nuevo pedido**: elige mesa y platos del menú ya cargado (ver
  `MenuRestaurante.java`), arma el carrito y crea el pedido.
- **Pedidos activos**: tabla con todos los pedidos creados en la sesión.
  Selecciona una fila y usa "Avanzar estado" o "Cancelar pedido".
- **Notificaciones**: dos paneles en vivo (cocina / mesero) que se llenan
  solos cada vez que cambias el estado de un pedido.
- **Analítica**: botones para generar los 3 reportes sobre los pedidos
  que hayas creado hasta ese momento.

Todo vive en memoria mientras la ventana esté abierta; al cerrarla se
pierde. Esto es intencional en el alcance de este corte (ver README.md,
sección 6, "límites honestos del diseño").

## Ejecutar la demo de consola (versión original, sin ventana)

```bash
java -cp out com.restaurant.Main
```

Si ves signos de interrogación en vez de tildes en la consola (por
ejemplo `Creaci?n` en vez de `Creación`), es solo la codificación de tu
terminal — no un error del programa. Puedes forzar UTF-8 así:

```bash
java -Dfile.encoding=UTF-8 -cp out com.restaurant.Main
```

## Qué hace la demo (`Main.java`)
1. Crea dos pedidos con distintos platos (usando `PlatoFactory`).
2. Avanza cada pedido por los 4 estados del flujo (`Creado → En
   preparación → Listo → Entregado`), notificando a cocina y mesero en
   cada transición (patrón Observer).
3. Calcula el total de cada pedido.
4. Genera tres reportes de consumo (platos más pedidos, menos pedidos y
   tiempo promedio de atención) usando el patrón Strategy.

## Siguientes pasos sugeridos (fuera de alcance de este corte)
- Persistencia en base de datos (reemplazando la lista en memoria de
  `GestorPedidos` por un repositorio).
- Interfaz de usuario (web o de escritorio) en vez de consola.
- Autenticación de meseros/administrador.
