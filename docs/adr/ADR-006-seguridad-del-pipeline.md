# ADR-006 — Estrategia de seguridad del pipeline: herramientas y umbrales

- **Estado:** aceptada
- **Fecha:** 2026-11-15

## Contexto

Hasta el Corte 2 la API no tenía autenticación y nadie revisaba las dependencias: el proyecto usaba
Spring Boot 2.7, sin soporte, con 20 vulnerabilidades altas y críticas conocidas. El Corte 3 exige
cuatro controles que detengan el pipeline (SAST, SCA, secretos con todo el historial y escaneo de la
imagen) con umbrales mínimos que se pueden endurecer pero no relajar, y un registro de cada hallazgo.

Criterios para elegir: gratuito para un repositorio público, versión fija (resultados
reproducibles), que corra dentro de GitHub Actions sin cuentas externas ni tokens adicionales, que
entienda Java/Maven, JavaScript e imágenes Docker, y que su resultado se pueda convertir en un
*quality gate*.

## Opciones consideradas

| Control | Opciones | Elegida | Por qué |
|---|---|---|---|
| SAST | CodeQL, Semgrep, SonarCloud | **CodeQL** (`security-extended`) | Análisis de flujo de datos para Java (sigue el valor desde la petición HTTP hasta el log o el SQL) y para el JavaScript del POS web; nativo de GitHub, sin cuenta externa. SonarCloud pide cuenta y token; Semgrep OSS no hace flujo entre métodos en Java |
| SCA | Trivy fs, OWASP Dependency-Check, dependency-review-action | **Trivy fs** | Una sola herramienta para SCA y para la imagen (misma base de datos, mismos criterios); Dependency-Check tarda más de 10 min en descargar la base NVD sin una API key; `dependency-review` solo mira el diff del PR, no el estado completo |
| Secretos | Gitleaks, TruffleHog | **Gitleaks** (`git`, `fetch-depth: 0`) | Recorre todo el historial, reglas para cientos de proveedores, SARIF como reporte; TruffleHog verifica contra los proveedores en línea (más lento y envía los candidatos fuera del runner) |
| Imagen | Trivy, Grype | **Trivy image** | La misma herramienta que SCA; detecta tanto paquetes del sistema operativo como el `.jar` |
| Actualizaciones | Dependabot, Renovate | **Dependabot** | Integrado en GitHub; **no reemplaza** el SCA, solo propone PR que pasan por el mismo pipeline |

## Decisión

| Control | Umbral mínimo del enunciado | Umbral aplicado |
|---|---|---|
| SAST | Ningún alto o crítico sin tratar | Hallazgo con `security-severity` ≥ 7.0 → falla. CodeQL solo reporta, así que [`umbral_sarif.py`](../../.github/scripts/umbral_sarif.py) lee el SARIF y falla. **Más estricto:** suite `security-extended` en vez de la por defecto, y JavaScript además de Java |
| SCA | Ninguna alta o crítica con corrección | HIGH o CRITICAL con corrección → falla. **Más estricto:** incluye dependencias de prueba |
| Secretos | Cero, incluido el historial | Cero, en todo el historial, en cada PR |
| Imagen | Ninguna crítica con corrección | CRITICAL con corrección → falla; el reporte completo queda como artefacto |

Reglas de tratamiento:

1. Cada hallazgo se registra en [seguridad.md §4](../seguridad.md#4-registro-de-hallazgos) como
   corregido, falso positivo o riesgo aceptado.
2. Un **riesgo aceptado** de Trivy va en `.trivyignore` con justificación y **fecha de
   vencimiento** (`exp:`); al vencer, el pipeline vuelve a fallar y obliga a revisarlo.
3. Cuando sea posible, un riesgo aceptado tiene un **control compensatorio automático**. Ejemplo: la
   regla ArchUnit R8 impide usar las piezas de Spring afectadas por CVE-2026-47884/47890.
4. No se deshabilitan reglas de CodeQL ni se suprimen hallazgos en el código: se corrige el código.
5. Todas las herramientas tienen versión fija (`aquasec/trivy:0.76.2`, `zricethezav/gitleaks:v8.30.1`,
   acciones `@v3`/`@v4`) para que un cambio de versión no cambie el resultado sin que se note
   (Dependabot propone actualizarlas).

## Consecuencias

**Positivas**
- La primera corrida del pipeline en GitHub ya encontró y bloqueó dos problemas reales del código
  (comparación `int`/`long` e inyección en el log), que se corrigieron antes de llegar a `main`.
- Una sola fuente de verdad para vulnerabilidades (Trivy) en dependencias e imagen.
- Los riesgos aceptados no se olvidan: vencen.

**Negativas**
- Las bases de vulnerabilidades cambian a diario: un PR puede fallar por una CVE publicada ayer sin
  haber tocado dependencias. Es intencional (pasó durante este corte con pgjdbc y Spring), pero
  obliga a reaccionar rápido; la vía es actualizar o aceptar con vencimiento.
- CodeQL agrega ≈ 1,5 min al pipeline (corre en paralelo con las pruebas).
- Trivy necesita las dependencias en el caché de Maven para analizar el `pom.xml` sin consultar Maven
  Central (que limita la tasa de descargas): el job resuelve dependencias primero.
