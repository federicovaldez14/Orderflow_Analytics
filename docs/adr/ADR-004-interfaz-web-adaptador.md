# ADR-004 — Interfaz web como tercer adaptador de entrada

- **Estado:** aceptada
- **Fecha:** 2026-09-28
- **Decisores:** Federico Valdez, Daniel Sanabria
- **Relacionada con:** [ADR-001](ADR-001-arquitectura-hexagonal.md) (hexagonal)

## Contexto

El POS de escritorio (Swing) cubre los retos, pero tiene tres límites:

1. **Usabilidad (R3 pide "que se vea mejor"):** Swing limita el diseño (sin tipografía ni gráficas
   modernas) y solo funciona en el computador donde corre la aplicación; un mesero no puede usarlo
   desde una tablet.
2. **Pruebas de UI:** Selenium y Cypress trabajan sobre un navegador; sobre Swing no hay forma de
   automatizar la interfaz con esas herramientas.
3. **Validar la arquitectura:** ADR-001 sostiene que se pueden agregar canales sin tocar el núcleo.
   Agregar uno es la mejor forma de comprobarlo.

No se busca funcionalidad de negocio nueva: la web expone las mismas operaciones que Swing y la API.

## Opciones consideradas

| Opción | A favor | En contra |
|---|---|---|
| A. Solo rediseñar Swing (FlatLaf) | Cero piezas nuevas | No resuelve 1 (otros dispositivos) ni 2 (pruebas de UI) |
| B. SPA aparte (React/Angular) con su propio proyecto y build | Ecosistema maduro, componentes | Segundo proyecto, Node y un build más; CORS o proxy; desproporcionado para 2 personas (mismo argumento de K6 en arquitectura.md §3) |
| **C. HTML/CSS/JS estático servido por el mismo Spring Boot, consumiendo la API REST** | Mismo despliegue y mismo puerto; sin build ni dependencias; reutiliza la API que ya prueban las pruebas de sistema y k6 | Sin framework: el estado de cada vista se maneja a mano |

## Decisión

Se elige **C**, y además se aplica **A** como retoque (FlatLaf oscuro, misma paleta que la web) para
que los dos clientes se vean como el mismo producto.

- La web vive en `src/main/resources/static/` (módulos ES: `api.js` es el cliente REST y `vistas/`
  tiene una vista por pantalla). No importa nada del núcleo: **solo habla HTTP/JSON**.
- La única pieza nueva del lado del servidor es un adaptador de lectura,
  `infraestructura/rest/NotificacionesController` (`GET /api/notificaciones`), que expone los
  mismos buffers que ya mostraba la pestaña *Notificaciones* de Swing.
- Las pruebas de UI usan Selenium con Page Object Model (`src/test/java/com/restaurant/uitests/`).

## Consecuencias

**Positivas**

- **Evidencia de la hexagonal:** se agregó un canal completo sin modificar `dominio/` ni
  `aplicacion/`; `ReglasDeDependenciaTest` sigue en verde. Swing, REST y web ejecutan las mismas
  reglas (una mesa ocupada da 409 igual en los tres).
- Usabilidad: el POS funciona en cualquier navegador, incluidas tablets y celulares de la red local.
- Las pruebas de UI automatizadas son posibles (bonificación) y corren en el CI con Chrome.

**Negativas (lo que se sacrifica)**

- La web consulta la API cada ~2,5 s (*polling*): con muchas pantallas abiertas genera carga
  constante. Lo correcto a futuro sería empujar cambios (WebSocket / Server-Sent Events).
- Sin autenticación: cualquiera en la red puede usar el POS (ya era un límite de la API, §7).
- Formatos y textos de presentación (pesos, nombres de estado) quedan repetidos entre Swing y web;
  son de presentación, no reglas de negocio.
- La tipografía Inter se descarga de Google Fonts: sin internet, la web usa la fuente del sistema.

## Cuándo reconsiderar

Si la web crece a muchas pantallas o necesita trabajar sin conexión, conviene pasar a un framework
(opción B) manteniendo la misma API REST como frontera.
