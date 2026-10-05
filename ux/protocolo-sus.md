# Protocolo de prueba con usuarios + cuestionario SUS

Complementa la [evaluación heurística](evaluacion-heuristica.md) con usuarios reales. Duración: unos
10 minutos por persona. Mínimo 3 usuarios que no hayan participado en el desarrollo.

## 1. Preparación

1. `mvn spring-boot:run` y abrir `http://localhost:8080` (escritorio, ventana completa).
2. Cada usuario parte de la aplicación recién arrancada (los datos se reinician al arrancar).
3. El observador **no ayuda**; anota tiempo, errores y si la tarea se completó.

## 2. Tareas (leer en voz alta, una a la vez)

| # | Tarea | Éxito si… |
|---|---|---|
| T1 | "La mesa 4 pidió 2 bandejas paisas y una limonada de coco. Envía la comanda a la cocina." | La mesa 4 queda en *Creado* con $73.000 |
| T2 | "Eres la cocina: lleva ese pedido hasta entregarlo." | La mesa 4 vuelve a *Libre* |
| T3 | "En la mesa 6 piden la cuenta (créala antes con cualquier plato): divídela en 3 partes iguales con 10 % de propina." | Aparecen 3 partes y la verificación en verde |
| T4 | "Contaste el aguacate y hay 3. Regístralo." | Aparece la alerta "Bajo el mínimo" |
| T5 | "¿Cuál fue el plato más pedido de la semana?" | Responde correctamente |

## 3. Registro por usuario

| Usuario | T1 (s / ¿éxito?) | T2 | T3 | T4 | T5 | Errores observados | SUS |
|---|---|---|---|---|---|---|---|
| U1 | | | | | | | |
| U2 | | | | | | | |
| U3 | | | | | | | |

**Métricas a reportar:** tasa de éxito por tarea, tiempo medio por tarea y SUS promedio.

## 4. Cuestionario SUS (System Usability Scale)

Escala de 1 (totalmente en desacuerdo) a 5 (totalmente de acuerdo):

1. Creo que me gustaría usar este sistema con frecuencia.
2. Encontré el sistema innecesariamente complejo.
3. Pensé que el sistema era fácil de usar.
4. Creo que necesitaría el apoyo de un técnico para poder usar este sistema.
5. Encontré que las distintas funciones del sistema estaban bien integradas.
6. Pensé que había demasiada inconsistencia en el sistema.
7. Imagino que la mayoría de las personas aprendería a usar este sistema muy rápido.
8. Encontré el sistema muy engorroso de usar.
9. Me sentí muy seguro(a) usando el sistema.
10. Necesité aprender muchas cosas antes de poder empezar a usar este sistema.

**Cálculo:** a las preguntas impares se les resta 1; a las pares, se resta su valor de 5. Se suman
los 10 resultados y se multiplica por 2,5 (puntaje de 0 a 100). Referencia: 68 es el promedio de la
industria; ≥ 80 es bueno.

## 5. Después de la prueba

Agregar a [`evaluacion-heuristica.md`](evaluacion-heuristica.md) los hallazgos nuevos con su
severidad y, si se corrige alguno, cómo se verificó.
