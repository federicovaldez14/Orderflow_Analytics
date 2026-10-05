# Evaluación de UX (bonificación)

| Archivo | Contenido |
|---|---|
| [`evaluacion-heuristica.md`](evaluacion-heuristica.md) | Evaluación heurística de Nielsen del POS web: 5 tareas, 8 hallazgos con severidad 0–4, métricas y 4 mejoras aplicadas (la principal verificada con Selenium) |
| [`protocolo-sus.md`](protocolo-sus.md) | Protocolo de prueba con 3 usuarios: tareas, registro de tiempos/éxito y cuestionario SUS |

La mejora de mayor severidad (H5-1: cambiar de mesa borraba la comanda sin enviar) tiene su prueba
automatizada: `src/test/java/com/restaurant/uitests/PosWebUiIT.java` →
`shouldKeepUnsentOrderWhenSwitchingTables`.
