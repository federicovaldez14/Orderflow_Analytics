# Guía de implementación y operación — Orderflow Analytics

**Para quién:** el equipo técnico del restaurante, que conoce Docker y Git pero no conoce este
proyecto. Siguiendo esta guía en orden se pasa de cuentas vacías a un sistema funcionando y
desplegado automáticamente, y se aprende a operarlo después.

**Convenciones:** los comandos están completos, listos para copiar. Lo que va entre `<ángulos>` se
reemplaza por el valor propio, por ejemplo `<usuario-github>`. Los comandos de terminal se escriben
para Bash (Linux, macOS o Git Bash en Windows).

| Parte | |
|---|---|
| [1. Qué se va a implementar](#1-qué-se-va-a-implementar) | [6. Verificación](#6-verificación) |
| [2. Requisitos previos](#2-requisitos-previos) | [7. Operación](#7-operación) |
| [3. Costos estimados](#3-costos-estimados) | [8. Solución de problemas](#8-solución-de-problemas) |
| [4. Configuración](#4-configuración) | [9. Recomendaciones para producción](#9-recomendaciones-para-producción) |
| [5. Instalación paso a paso](#5-instalación-paso-a-paso) | [Anexo: instalación en un servidor propio](#anexo-instalación-en-un-servidor-propio-con-docker-compose) |

---

## 1. Qué se va a implementar

Orderflow Analytics es el sistema de pedidos del restaurante. Ofrece:

- el mapa de mesas y la toma de pedidos;
- el flujo de comandas con cocina;
- el inventario de ingredientes, que se descuenta con cada venta y deja un historial;
- la división de la cuenta;
- la analítica de ventas con gráficas.

Meseros, cocina y administración lo usan desde el navegador de un PC, una tablet o un celular, cada
uno con su usuario y su rol.

Componentes que se instalan:

| Componente | Qué es | Dónde corre |
|---|---|---|
| **Aplicación** | Un contenedor Docker con la API REST, el POS web y las métricas (Spring Boot, Java 17) | Render (servicio web) |
| **Base de datos** | PostgreSQL 18 | Neon (servicio gestionado) |
| **Registro de imágenes** | Las imágenes de cada versión | GitHub Container Registry (GHCR) |
| **Pipeline** | Pruebas, controles de seguridad, despliegue y reversión | GitHub Actions |
| *Opcional:* POS de escritorio | La misma aplicación con ventana Swing, como `.jar` | PC de la caja |

Diagrama de dónde corre cada pieza y cómo se comunican: [vista de despliegue](arquitectura.md#8-vista-de-despliegue-corte-3).
Si se prefiere un servidor propio en lugar de la nube, ver el
[anexo](#anexo-instalación-en-un-servidor-propio-con-docker-compose).

## 2. Requisitos previos

**Cuentas** (todas tienen plan gratuito):

| Cuenta | Para qué | Notas |
|---|---|---|
| [GitHub](https://github.com) | Código, pipeline (Actions) y registro de imágenes (GHCR) | El repositorio debe ser **público** para que Actions y GHCR sean gratuitos sin límite, o usar una organización con plan pago |
| [Render](https://render.com) | Correr la aplicación | Se puede entrar con la cuenta de GitHub |
| [Neon](https://neon.tech) | Base de datos PostgreSQL | Se puede entrar con la cuenta de GitHub |

**Software en el computador del administrador:**

| Software | Versión | Para qué |
|---|---|---|
| Git | 2.40 o superior | Clonar el repositorio y publicar versiones |
| Docker (Desktop o Engine) | 24 o superior, con Compose v2 | Probar localmente, copias de seguridad (`pg_dump`) |
| OpenSSL (incluido en Git Bash, Linux y macOS) | cualquiera | Generar claves |
| *Opcional:* GitHub CLI `gh` | 2.40 o superior | Configurar secretos desde la terminal |
| *Opcional:* Java | 17 o superior | Solo para el POS de escritorio |

**Recursos mínimos de la aplicación:** 0,1 CPU y 512 MB de RAM (plan Free de Render); recomendado
0,5 CPU y 512 MB–1 GB. **Base de datos:** 0,5 GB alcanzan para varios años de pedidos de un
restaurante.

**Puertos:** la aplicación escucha en el **8080** dentro del contenedor; Render la publica en el
443 (HTTPS). PostgreSQL usa el 5432 (Neon solo acepta conexiones TLS).

**Dominio y HTTPS:** Render entrega `https://<nombre-servicio>.onrender.com` con certificado
automático. Un dominio propio es opcional (Render → *Settings* → *Custom Domains*; el certificado
también es automático).

## 3. Costos estimados

Tarifas públicas de cada proveedor a noviembre de 2026, en dólares por mes; conviene confirmarlas en
la página de precios de cada uno antes de contratar.

| Componente | Plan usado en el proyecto | Costo | Plan recomendado para el restaurante | Costo |
|---|---|---|---|---|
| Aplicación (Render) | Free: 512 MB, 0,1 CPU, **se suspende tras 15 min sin uso** | USD 0 | **Starter**: 512 MB, 0,5 CPU, siempre encendido | ≈ USD 7 |
| Base de datos (Neon) | Free: 0,5 GB, cómputo que se suspende sin uso | USD 0 | **Launch** (pago por uso; un restaurante usa poco cómputo) | ≈ USD 5–20 |
| Registro de imágenes (GHCR) | Paquete público | USD 0 | Igual | USD 0 |
| Pipeline (GitHub Actions) | Repositorio público: minutos ilimitados | USD 0 | Igual (repositorio privado: 2.000 min/mes gratis, alcanza para ≈ 150 ejecuciones) | USD 0 |
| Dominio propio (opcional) | — | — | Dominio `.com` o `.co` | ≈ USD 1–3 |
| **Total** | | **USD 0** | | **≈ USD 12–30 / mes** |

¿Por qué no basta el plan gratuito para operar? Porque el servicio se duerme: el primer pedido
después de 15 minutos sin uso tardaría alrededor de un minuto en responder.

## 4. Configuración

Ninguna clave va en el código ni en la imagen: todo se configura con variables de entorno.

### 4.1 Variables de la aplicación (panel de Render → *Environment*)

| Variable | Para qué sirve | Ejemplo | ¿Obligatoria? |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Dirección JDBC de la base | `jdbc:postgresql://ep-xxxx.us-west-2.aws.neon.tech/neondb?sslmode=require&channelBinding=require` | **Sí** (sin ella usa una base en memoria que se borra) |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base | `neondb_owner` | **Sí** |
| `SPRING_DATASOURCE_PASSWORD` | Clave de la base (**secreto**) | la que entrega Neon | **Sí** |
| `ORDERFLOW_ADMIN_USUARIO` | Nombre del primer administrador | `admin` | No (por defecto `admin`) |
| `ORDERFLOW_ADMIN_CLAVE` | Clave del primer administrador (**secreto**, mínimo 12 caracteres). Solo se usa si **no existe** ningún administrador | generada (§4.4) | **Sí** (sin ella se genera una al azar y se muestra una vez en el log) |
| `ORDERFLOW_COOKIE_SEGURA` | Cookie de sesión solo por HTTPS | `true` | **Sí en Render** (`false` solo para HTTP local) |
| `ORDERFLOW_MESAS` | Número de mesas del salón | `10` | No (10) |
| `ORDERFLOW_DEMO_HISTORICO` | Carga 7 días de pedidos de ejemplo si la base está vacía | `false` | No (`true`). **Poner `false` en producción real** |
| `PORT` | Puerto en el que Render busca la aplicación | `8080` | **Sí en Render** |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | Conexiones simultáneas a la base | `5` | No (20); en el plan Free usar 5 |
| `ORDERFLOW_MONITOR_USUARIO` / `ORDERFLOW_MONITOR_CLAVE` | Usuario de solo lectura de métricas para Prometheus (**secreto**, mínimo 12) | `prometheus` / generada | No (solo con observabilidad) |

La imagen ya trae `ORDERFLOW_UI_ENABLED=false` (sin ventana) y `ORDERFLOW_VERSION=<commit>`.

### 4.2 Secretos y variables del pipeline (GitHub → *Settings* → *Environments* → `produccion`)

| Nombre | Tipo | Para qué | Cómo se obtiene |
|---|---|---|---|
| `RENDER_DEPLOY_HOOK` | *Environment secret* | URL secreta con la que el pipeline ordena desplegar | Render → servicio → *Settings* → *Deploy Hook* |
| `ORDERFLOW_ADMIN_CLAVE` | *Environment secret* | La prueba de humo y la de carga inician sesión como administrador | **La misma clave** del administrador configurada en Render |
| `URL_DESPLIEGUE` | *Environment variable* | Dirección pública del sistema | Render → servicio → URL arriba a la izquierda, sin `/` final |

`GITHUB_TOKEN` (para publicar en GHCR y crear Releases) lo crea GitHub solo; no hay que configurarlo.

### 4.3 Variables de Docker Compose (archivo `.env`, solo instalación local o servidor propio)

Todas están en [`.env.example`](../.env.example) con valores de ejemplo: `PUERTO`, `POSTGRES_DB`,
`POSTGRES_USER`, `POSTGRES_PASSWORD`, `ORDERFLOW_ADMIN_USUARIO`, `ORDERFLOW_ADMIN_CLAVE`,
`ORDERFLOW_MESAS`, `ORDERFLOW_DEMO_HISTORICO`, `ORDERFLOW_MONITOR_USUARIO`,
`ORDERFLOW_MONITOR_CLAVE`, `GRAFANA_USUARIO` y `GRAFANA_CLAVE`. El archivo `.env` nunca se sube al
repositorio (está en `.gitignore`).

### 4.4 Cómo generar los valores sensibles

```bash
openssl rand -base64 24
```

Genera una clave aleatoria de 32 caracteres; una por cada secreto. Guardarlas en el gestor de
contraseñas del restaurante, **nunca** en un archivo del repositorio ni en un chat.

## 5. Instalación paso a paso

Tiempo estimado: 45 minutos.

**Paso 1 — Copiar el repositorio a la cuenta del restaurante.**
En <https://github.com/federicovaldez14/Orderflow_Analytics> → **Fork** → elegir la cuenta u
organización del restaurante. Luego en el computador:

```bash
git clone https://github.com/<usuario-github>/Orderflow_Analytics.git
cd Orderflow_Analytics
```

*Resultado:* una carpeta `Orderflow_Analytics` con el código. En el fork, abrir la pestaña
**Actions** y pulsar **I understand my workflows, go ahead and enable them**.

**Paso 2 — Crear la base de datos en Neon.**
1. En Neon: **New Project**; *Name* `orderflow`; *Region* **AWS US West 2 (Oregon)**; dejar
   *Postgres database* activado y **no** activar *Better Auth*. **Create**.
2. **Connect** → *Branch* `production`, *Database* `neondb`, *Role* `neondb_owner`,
   **Connection pooling apagado** → copiar la cadena de conexión.

*Resultado:* una cadena así:
`postgresql://neondb_owner:<clave>@ep-xxxx.us-west-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require`.
De ella salen los tres valores de la base de la tabla §4.1:

- la URL JDBC (`jdbc:postgresql://` + host + `/neondb?sslmode=require&channelBinding=require`);
- el usuario;
- la clave.

**Paso 3 — Generar las claves.** Correr `openssl rand -base64 24` una vez para la clave del
administrador (§4.4) y guardarla.

**Paso 4 — Construir y publicar la primera imagen.**
En GitHub: **Actions → Pipeline → Run workflow** → *Branch* `main` → **Run workflow**.

*Resultado:* las etapas 1 a 7 terminan en verde (≈ 7 min) y la imagen queda en
`ghcr.io/<usuario-github>/orderflow-analytics:sha-<commit>`. La etapa **8 · Despliegue** falla esta
primera vez porque Render todavía no existe: es lo esperado.

**Paso 5 — Hacer pública la imagen.**
GitHub → foto de perfil → **Your profile** → **Packages** → `orderflow-analytics` →
**Package settings** → *Danger Zone* → **Change visibility** → **Public**.

*Resultado:* Render puede descargar la imagen sin credenciales. Si el repositorio es privado y se
prefiere un paquete privado, en el paso 6 agregar en Render una *Registry Credential* con un token de
GitHub con permiso `read:packages`.

**Paso 6 — Crear el servicio en Render.**
1. **New → Web Service → Existing Image**.
2. *Image URL*: `ghcr.io/<usuario-github>/orderflow-analytics:sha-<commit>`. El nombre exacto está
   en la corrida del paso 4: job *7 · Imagen y escaneo* → paso *Nombre de la imagen*.
3. *Name* `orderflow-analytics`; *Region* **Oregon (US West)**; *Instance Type* **Free** o
   **Starter** (§3).
4. *Environment Variables*: todas las obligatorias de la tabla §4.1.
5. *Advanced*: *Health Check Path* `/actuator/health/readiness`; *Auto-Deploy* **No**.
6. **Deploy Web Service**.

*Resultado:* en *Logs* aparece `Usuario administrador inicial 'admin' creado` y
`Started OrderflowApplication`, y el estado pasa a **Live** (2–4 minutos en el plan Free).
**Datos iniciales:** en el primer arranque la aplicación crea sus tablas en Neon, carga la carta
(10 platos), 20 ingredientes con sus recetas y el primer administrador. Con
`ORDERFLOW_DEMO_HISTORICO=true` también carga pedidos de ejemplo para la analítica.

**Paso 7 — Conectar el pipeline con Render.**
1. Render → servicio → **Settings → Deploy Hook** → copiar la URL.
2. GitHub → repositorio → **Settings → Environments → New environment** → nombre `produccion` →
   **Configure environment**.
3. En *Environment secrets*, **Add secret**:
   - `RENDER_DEPLOY_HOOK`: la URL del hook;
   - `ORDERFLOW_ADMIN_CLAVE`: la clave del paso 3.
4. En *Environment variables*, **Add variable**: `URL_DESPLIEGUE` =
   `https://<nombre-servicio>.onrender.com`.

Con GitHub CLI, el mismo paso desde la terminal (pide pegar cada valor; no queda en el historial):

```bash
gh secret set RENDER_DEPLOY_HOOK --env produccion
gh secret set ORDERFLOW_ADMIN_CLAVE --env produccion
gh variable set URL_DESPLIEGUE --env produccion --body "https://<nombre-servicio>.onrender.com"
```

**Paso 8 — Primer despliegue automático.**
En la corrida del paso 4 → **Re-run failed jobs**.

*Resultado:* la etapa 8 queda en verde. En su resumen está el reporte de la prueba de humo, con 10
verificaciones ✅, y el de la prueba de carga contra el SLO.

**Paso 9 — Proteger la rama principal.**
GitHub → **Settings → Rules → Rulesets → New branch ruleset**:

- *Name* `proteger-main`; *Enforcement* **Active**; *Target branches* → **Include default branch**.
- Marcar:
  - **Restrict deletions**;
  - **Block force pushes**;
  - **Require a pull request before merging**, con *Required approvals* `1` y *Dismiss stale
    pull request approvals when new commits are pushed*;
  - **Require status checks to pass**, con *Require branches to be up to date*. Agregar los
    checks del pipeline: `1 · Construcción`, `2 · Pruebas unitarias y cobertura`,
    `3 · Pruebas de integración`, `4 · Reglas de arquitectura`, `5 · Pruebas de UI (Selenium)`,
    `6a · SAST (CodeQL java-kotlin)`, `6a · SAST (CodeQL javascript-typescript)`,
    `6b · SCA (dependencias, Trivy)`, `6c · Secretos (Gitleaks, todo el historial)` y
    `7 · Imagen y escaneo (Trivy)`.
- **Create**.

*Resultado:* nadie puede hacer push directo a `main`; todo cambio entra por pull request con el
pipeline en verde y la aprobación de otra persona.

**Paso 10 — Crear los usuarios del restaurante.**
Entrar a `https://<nombre-servicio>.onrender.com` con el administrador. Por ahora los usuarios se
crean por la API, con la sesión del administrador o con HTTP Basic:

```bash
curl -u admin:<clave-admin> -H "Content-Type: application/json" \
  -d '{"usuario":"mesero1","clave":"<clave-de-12-o-mas>","rol":"MESERO"}' \
  https://<nombre-servicio>.onrender.com/api/usuarios
```

Los roles posibles son `MESERO`, `COCINA`, `ADMIN` y `MONITOR`.

*Resultado:* respuesta `201` con el usuario creado. `GET /api/usuarios` lista los existentes.

**Paso 11 (opcional) — POS de escritorio.**
1. GitHub → **Releases** → última versión → descargar `orderflow-analytics-<versión>.jar`.
2. En el PC de la caja, con Java 17 o superior:

   ```bash
   java -jar orderflow-analytics-<versión>.jar
   ```

   Así abre la ventana con una base local propia.
3. Para que comparta los datos con el sistema web, conectarlo a la misma base:

   ```bash
   SPRING_DATASOURCE_URL="<url-jdbc-de-neon>" SPRING_DATASOURCE_USERNAME=neondb_owner SPRING_DATASOURCE_PASSWORD="<clave-neon>" ORDERFLOW_DEMO_HISTORICO=false java -jar orderflow-analytics-<versión>.jar
   ```

   En Windows PowerShell las variables se definen antes con `$env:SPRING_DATASOURCE_URL="..."`.

## 6. Verificación

1. **Healthcheck** (debe responder `{"status":"UP"}`):

   ```bash
   curl https://<nombre-servicio>.onrender.com/actuator/health/readiness
   ```

2. **Versión desplegada** (debe mostrar el commit de `main`):

   ```bash
   curl https://<nombre-servicio>.onrender.com/actuator/info
   ```

3. **Prueba de humo** (la misma que corre el pipeline; requiere k6 o Docker):

   ```bash
   docker run --rm -v "$PWD:/w" -w /w -e BASE_URL=https://<nombre-servicio>.onrender.com -e ORDERFLOW_CLAVE=<clave-admin> grafana/k6:2.4.0 run perf/scripts/humo_k6.js
   ```

   *Resultado:* 10 verificaciones ✅ (servicio listo, web, sesión, carta, pedido que descuenta
   inventario, trazabilidad, pedido entregado, cuenta que cuadra, analítica y cierre de sesión).
4. **Recorrido corto en el navegador:**
   1. Entrar como administrador.
   2. **Salón**: elegir una mesa libre, agregar dos platos y **Enviar a cocina**.
   3. **Cocina**: avanzar el pedido hasta *Listo* y luego entregarlo.
   4. **Inventario**: los ingredientes bajaron y la mesa aparece en *Movimientos*.
   5. **Dividir cuenta**: entre 3 personas con 10 % de propina; la suma cuadra con el total.
   6. **Analítica**: el pedido aparece en las ventas de hoy.
   7. Cerrar sesión, entrar como un usuario `COCINA` y comprobar que solo ve *Cocina* y *Actividad*.

## 7. Operación

**Publicar una nueva versión**
1. Rama nueva desde `main`, cambios, pull request. El pipeline corre solo.
2. Otra persona revisa y aprueba; con todo en verde, **Merge pull request**. El pipeline de `main`
   despliega solo y verifica con la prueba de humo y la de carga.
3. Para marcar la versión, actualizar `CHANGELOG.md` en ese mismo pull request. Después del merge:

   ```bash
   git checkout main && git pull
   git tag v<X.Y.Z>
   git push origin v<X.Y.Z>
   ```

   *Resultado:* el workflow **Versión** etiqueta la imagen como `:v<X.Y.Z>` y crea el GitHub
   Release con el `.jar`.

**Volver a la versión anterior**
GitHub → **Actions → Reversión → Run workflow** → escribir el tag anterior (por ejemplo `v3.0.0`)
→ **Run workflow**. En ≈ 3 min producción vuelve a esa imagen y la prueba de humo lo confirma.

Sin GitHub Actions: Render → *Settings* → *Image URL* =
`ghcr.io/<usuario-github>/orderflow-analytics:v3.0.0` → *Save* → **Manual Deploy**. Detalle en
[devops.md §6](devops.md#6-reversión).

**Copia de seguridad de los datos**
- *Automática:* Neon guarda el historial de cambios y permite restaurar a un punto en el tiempo
  (*Branches* → *Restore*), dentro de la ventana que da el plan (horas en Free, días en los pagos).
- *Manual*, con un archivo descargado (no requiere instalar PostgreSQL):

  ```bash
  docker run --rm -v "$PWD:/respaldo" postgres:18.7-alpine pg_dump "postgresql://neondb_owner:<clave-neon>@<host-neon>/neondb?sslmode=require" -Fc -f /respaldo/orderflow-$(date +%F).dump
  ```

  *Resultado:* un archivo `orderflow-AAAA-MM-DD.dump` en la carpeta actual. Guardarlo fuera del
  computador (unidad de la empresa).

**Restaurar una copia**

```bash
docker run --rm -v "$PWD:/respaldo" postgres:18.7-alpine pg_restore --clean --if-exists --no-owner -d "postgresql://neondb_owner:<clave-neon>@<host-neon>/neondb?sslmode=require" /respaldo/orderflow-<fecha>.dump
```

Después, Render → **Manual Deploy → Restart service** para vaciar las cachés de la aplicación.

**Cambiar un secreto**

| Secreto | Cómo cambiarlo |
|---|---|
| Clave de la base | Neon → *Branches* → `production` → *Roles* → `neondb_owner` → **Reset password**. Copiar la nueva en Render → *Environment* → `SPRING_DATASOURCE_PASSWORD` → *Save* (Render reinicia solo) |
| Deploy hook (si se filtró) | Render → *Settings* → *Deploy Hook* → **Regenerate**. Actualizar `RENDER_DEPLOY_HOOK` en el environment `produccion` de GitHub |
| Clave del administrador | La variable solo crea el **primer** administrador. Para cambiarla: crear un administrador nuevo con `POST /api/usuarios`; o, si se perdió, borrar el administrador en Neon (*SQL Editor*: `DELETE FROM authorities WHERE username='admin'; DELETE FROM users WHERE username='admin';`), poner la clave nueva en `ORDERFLOW_ADMIN_CLAVE` de Render y reiniciar: se vuelve a crear. Actualizar también `ORDERFLOW_ADMIN_CLAVE` en GitHub |

**Consultar los logs**
Render → servicio → **Logs**, con búsqueda y filtro por fecha. Las acciones de los usuarios están en
las líneas `AUDITORIA`, por ejemplo `usuario=mesero1 POST /api/pedidos -> 201`.

**Reiniciar el servicio**
Render → servicio → **Manual Deploy → Restart service**. Los datos no se pierden, porque están en
Neon.

## 8. Solución de problemas

| # | Síntoma | Causa | Solución |
|---|---|---|---|
| 1 | En los logs de Render: `password authentication failed for user 'neondb_owner'` y `Exited with status 1` | `SPRING_DATASOURCE_PASSWORD` no coincide con la clave actual de Neon: espacios de más al pegarla, se copió la cadena entera o se hizo *Reset password* después de configurarla | En Render → *Environment*, pegar **solo** la clave (lo que está entre `neondb_owner:` y `@`) sin espacios ni comillas → *Save* |
| 2 | Render muestra `No open ports detected` y el despliegue termina por tiempo | Falta `PORT=8080`, o la aplicación se detuvo antes de abrir el puerto (ver el error anterior en el log) | Agregar `PORT=8080`; si hay un error de arranque en el log, resolverlo primero |
| 3 | La etapa *8 · Despliegue* falla con `La versión <sha> no quedó en línea en 900 s` | Render no pudo descargar la imagen (paquete privado), el deploy hook no es el del servicio, o la aplicación no arrancó | Hacer público el paquete (paso 5); regenerar y copiar de nuevo el hook (§7); revisar los logs de Render |
| 4 | La prueba de humo falla en `sesión iniciada como administrador` (401) | `ORDERFLOW_ADMIN_CLAVE` de GitHub no es la clave real del administrador, que solo se fija la primera vez | Poner en GitHub la clave real, o recrear el administrador como se explica en §7 |
| 5 | En el navegador: "Falta el token de seguridad o la sesión venció" al guardar | La sesión expiró (8 h), el navegador bloquea cookies, o `ORDERFLOW_COOKIE_SEGURA=true` mientras se entra por `http://` (instalación local) | Recargar la página y entrar de nuevo; permitir cookies del sitio; en local por HTTP usar `ORDERFLOW_COOKIE_SEGURA=false` |
| 6 | `docker compose up` falla con `Defina POSTGRES_USER (ver .env.example)` | No existe el archivo `.env` | `cp .env.example .env` y cambiar las claves |
| 7 | La aplicación no arranca: `ORDERFLOW_ADMIN_CLAVE debe tener al menos 12 caracteres` | `ORDERFLOW_ADMIN_CLAVE` es muy corta | Generar una con `openssl rand -base64 24` |
| 8 | Un pull request falla en *6b · SCA* o *7 · Imagen* sin haber cambiado dependencias | Se publicó una vulnerabilidad nueva en una librería o en la imagen base | Ver el artefacto `reporte-sca` / `reporte-imagen`; subir la versión de la librería (o aceptar el pull request de Dependabot). Si no hay corrección aplicable, registrar la decisión en `docs/seguridad.md` y en `.trivyignore` con fecha de vencimiento |
| 9 | La primera página tarda alrededor de un minuto en cargar | Plan Free: el servicio se suspendió tras 15 min sin uso | Esperar; para operar a diario usar el plan Starter (§3) |
| 10 | Render reinicia la aplicación con `Out of memory` | 512 MB es poco para muchas consultas simultáneas | Bajar `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` a 5 y pasar a una instancia con 1 GB o más |

## 9. Recomendaciones para producción

Respecto al entorno del proyecto, el restaurante debe cambiar:

1. **Plan de los servicios:** Render Starter o superior, para que no se suspenda, y Neon Launch con
   una ventana de restauración de al menos 7 días.
2. **Credenciales:**
   - generar claves nuevas para todo (§4.4), sin reutilizar las del proyecto;
   - crear un usuario por persona, nunca compartir el `admin`;
   - poner `ORDERFLOW_DEMO_HISTORICO=false` antes del primer arranque para no mezclar pedidos de
     ejemplo con ventas reales.
3. **HTTPS:** ya viene con Render. Con dominio propio, configurarlo en Render (certificado
   automático) y mantener `ORDERFLOW_COOKIE_SEGURA=true`.
4. **Copias de seguridad programadas:** además de la restauración de Neon, un `pg_dump` semanal
   (§7) guardado fuera del proveedor. Probar una restauración al menos una vez al trimestre.
5. **Tamaño de los recursos:** con más de 3–4 pantallas abiertas a la vez o más de 50.000 pedidos
   en el historial, usar 1 GB de RAM para la aplicación.
6. **Acceso a GitHub:**
   - autenticación en dos pasos para todas las cuentas con permiso de escritura;
   - mantener la regla de protección de `main`;
   - revisar cada semana los pull requests de Dependabot.
7. **Monitoreo:** activar las alertas de Render por caídas. Opcionalmente, conectar Prometheus y
   Grafana con el tablero del SLO incluido en `observabilidad/`, que en local se levanta con
   `docker compose --profile observabilidad up -d`.

**Riesgos aceptados que siguen abiertos** ([seguridad.md §7](seguridad.md#7-riesgos-abiertos)):

- Spring Framework 6.2 con dos CVE sin parche en esa rama (H-07). No afectan porque el sistema no
  usa vistas XSLT ni SSE, y una regla automática impide empezar a usarlas. La excepción vence el
  2027-02-28; antes hay que migrar a Spring Boot 4.
- No hay límite de intentos de inicio de sesión.
- La auditoría vive en los logs de Render, que el plan gratuito conserva por poco tiempo.

---

## Anexo: instalación en un servidor propio con Docker Compose

Para correr todo en un servidor del restaurante (Linux con Docker, 1 vCPU, 2 GB de RAM y 10 GB de
disco), sin Render ni Neon:

```bash
git clone https://github.com/<usuario-github>/Orderflow_Analytics.git
cd Orderflow_Analytics
cp .env.example .env
nano .env
docker compose up -d --build
docker compose ps
```

En `.env`, cambiar todas las claves (§4.4).

*Resultado:* `db` y `app` en estado `healthy`. El sistema queda en `http://<ip-servidor>:8080`.
Mientras se entre por HTTP, poner `ORDERFLOW_COOKIE_SEGURA=false` en el servicio `app`; para HTTPS,
poner delante un proxy inverso con certificado (Caddy o Nginx con Let's Encrypt) y volver a `true`.

| Tarea | Comando |
|---|---|
| Ver logs | `docker compose logs -f app` |
| Reiniciar | `docker compose restart app` |
| Copia de seguridad | `docker compose exec db pg_dump -U <POSTGRES_USER> -Fc <POSTGRES_DB> > orderflow-$(date +%F).dump` |
| Restaurar | `docker compose exec -T db pg_restore -U <POSTGRES_USER> -d <POSTGRES_DB> --clean --if-exists < orderflow-<fecha>.dump` |
| Actualizar a una versión publicada | `git fetch --tags && git checkout v<X.Y.Z> && docker compose up -d --build` |
| Con métricas y tablero | `docker compose --profile observabilidad up -d` → Grafana en `http://localhost:3000` (solo desde el servidor) |
