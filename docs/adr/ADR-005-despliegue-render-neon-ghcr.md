# ADR-005 — Despliegue continuo en Render con PostgreSQL en Neon e imágenes en GHCR

- **Estado:** aceptada
- **Fecha:** 2026-11-15
- **Reemplaza en parte a:** [ADR-002](ADR-002-persistencia-jdbc-h2-pool.md) (H2 deja de ser la base de producción)

## Contexto

En el Corte 2 el sistema corría en el computador de un integrante, con H2 en memoria: los datos se
perdían al reiniciar y la base compartía el heap de la aplicación (límite declarado en
[arquitectura.md §7](../arquitectura.md#7-límites-conocidos-y-trabajo-para-el-corte-3)). El Corte 3
exige que cada cambio que llegue a `main` se despliegue sin pasos manuales en un entorno accesible,
con la imagen etiquetada por SHA, secretos fuera del repositorio, prueba de humo, prueba de carga
contra el SLO y reversión probada.

Restricciones: presupuesto cero (proyecto académico), repositorio público en GitHub, un solo
contenedor de aplicación (la hexagonal es un monolito modular,
[ADR-001](ADR-001-arquitectura-hexagonal.md)) y una base relacional con el mismo SQL que ya usan los
adaptadores JDBC.

## Opciones consideradas

| Opción | A favor | En contra |
|---|---|---|
| **Render (imagen de GHCR) + Neon PostgreSQL** | Plan gratuito sin tarjeta; despliega una **imagen ya construida** (no reconstruye: lo que se escaneó es lo que corre); *deploy hook* con `imgURL` permite desplegar y revertir a una imagen exacta; HTTPS incluido; Neon da PostgreSQL 18 gratis con TLS y copias por *branching* | El servicio gratuito se duerme tras 15 min sin tráfico (≈ 1 min para despertar); 512 MB y 0,1 CPU; la base de datos gratuita de Render expira, por eso la base va en Neon |
| Railway | Despliegue sencillo, base incluida | Sin plan gratuito permanente (crédito de prueba) |
| Fly.io | Máquinas cerca de los usuarios, volúmenes | Exige tarjeta; más configuración (`fly.toml`, redes) |
| Azure for Students / AWS Academy | Servicios "reales" de empresa | Crédito limitado y cuentas que expiran; más piezas (registro, App Service/ECS, red, IAM) para el mismo resultado |
| Servidor propio + runner autohospedado + Docker Compose | Control total, mismo `docker-compose.yml` del desarrollo | Depende de que un PC del grupo esté prendido y expuesto a Internet; el runner autohospedado en un repositorio público es un riesgo (ejecuta código de cualquier PR) |
| Kubernetes (kind/minikube) | Bonificación, probes y límites | No es un entorno accesible desde Internet sin más infraestructura |

Para el registro se compararon **GHCR** (integrado con `GITHUB_TOKEN`, sin credenciales extra,
gratuito para paquetes públicos) y Docker Hub (límite de descargas, otra cuenta y otro secreto).

## Decisión

- **Imagen:** el pipeline construye la imagen multi-stage una sola vez (job 7), la escanea y, solo en
  `main`, la publica como `ghcr.io/federicovaldez14/orderflow-analytics:sha-<commit>`. Un tag
  `vX.Y.Z` agrega la etiqueta de versión **a esa misma imagen** (`release.yml`, sin reconstruir).
  Nunca se usa `latest`.
- **Aplicación:** servicio web de Render (región Oregon, plan Free) que corre la imagen de GHCR, con
  *health check* en `/actuator/health/readiness` y **Auto-Deploy apagado**: solo despliega cuando el
  job 8 llama al *deploy hook* con la imagen exacta.
- **Base de datos:** Neon PostgreSQL 18 (región AWS us-west-2, la misma de Render) con TLS y
  *channel binding*; los mismos adaptadores JDBC, que detectan PostgreSQL para la secuencia de ids.
  H2 queda para las pruebas rápidas y el modo de escritorio sin servidor.
- **Verificación:** el job 8 espera a que `/actuator/info` responda el SHA desplegado, corre la prueba
  de humo y luego la carga corta con los umbrales del SLO. Si algo falla, el pipeline falla.
- **Reversión:** volver a desplegar la imagen de un tag anterior (`reversion.yml`).
- **Un solo entorno** (producción) con *environment* `produccion` en GitHub para los secretos; sin
  aprobación manual: la aprobación ocurre en el pull request.

## Consecuencias

**Positivas**
- Lo que se despliega es byte a byte lo que pasó las pruebas y los escaneos (mismo digest).
- Reversión en minutos y sin reconstruir: las imágenes de cada versión quedan en GHCR.
- Los datos sobreviven a reinicios y despliegues (Neon), y la base ya no compite por el heap.
- El mismo `docker-compose.yml` levanta localmente una réplica fiel (app + PostgreSQL 18).

**Negativas**
- El plan gratuito se duerme: la primera petición tras 15 min tarda ≈ 1 min. El pipeline lo tolera
  (espera al *readiness*), pero un usuario real lo notaría; para el negocio se recomienda el plan
  *Starter* ([implementacion.md §3](../implementacion.md#3-costos-estimados)).
- 0,1 CPU: el arranque de Spring Boot tarda ≈ 1 min y la prueba de carga corta usa pocos usuarios
  (detecta regresiones, no mide capacidad).
- Dependencia de dos proveedores externos; si Render o Neon cambian sus planes gratuitos hay que
  migrar. Mitigación: todo es una imagen Docker estándar y PostgreSQL estándar; la guía de
  implementación describe también la instalación con Docker Compose en un servidor propio.
- Un solo entorno: un error que pase todos los controles llega directo a producción. Mitigación:
  reversión probada y prueba de humo después de cada despliegue.
