#!/usr/bin/env bash
# Despliega una imagen de GHCR en Render y espera a que el servicio responda
# con esa versión. Lo usan el pipeline (cada cambio en main) y la reversión.
#
#   desplegar.sh <imagen> <version-esperada>
#     imagen            ghcr.io/<usuario>/orderflow-analytics:sha-<sha> (o :vX.Y.Z)
#     version-esperada  SHA del commit con el que se construyó la imagen
#
# Variables: RENDER_DEPLOY_HOOK (secreto: URL del deploy hook de Render)
#            URL_DESPLIEGUE    (URL pública del servicio)
set -euo pipefail

IMAGEN="$1"
VERSION="$2"
: "${RENDER_DEPLOY_HOOK:?Falta el secreto RENDER_DEPLOY_HOOK}"
: "${URL_DESPLIEGUE:?Falta la variable URL_DESPLIEGUE}"
MAX_SEGUNDOS="${MAX_SEGUNDOS:-900}"

# Render despliega exactamente esta imagen (parámetro imgURL del deploy hook)
imagen_codificada=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1], safe=''))" "$IMAGEN")
echo "Desplegando $IMAGEN en Render…"
curl -fsS -X POST "${RENDER_DEPLOY_HOOK}&imgURL=${imagen_codificada}" -o /dev/null

# Espera a que /actuator/info responda con la versión nueva (el plan gratuito
# puede tardar en descargar la imagen, arrancar o despertar el servicio)
inicio=$(date +%s)
while true; do
  actual=$(curl -fsS --max-time 15 "${URL_DESPLIEGUE}/actuator/info" 2>/dev/null \
    | python3 -c "import json,sys; print(json.load(sys.stdin).get('app',{}).get('version',''))" 2>/dev/null || true)
  if [ "$actual" = "$VERSION" ]; then
    echo "Versión $VERSION en línea en ${URL_DESPLIEGUE} ($(( $(date +%s) - inicio )) s)"
    break
  fi
  if [ $(( $(date +%s) - inicio )) -ge "$MAX_SEGUNDOS" ]; then
    echo "::error::La versión $VERSION no quedó en línea en ${MAX_SEGUNDOS} s (versión actual: '${actual:-sin respuesta}')"
    exit 1
  fi
  echo "Esperando la versión nueva… (actual: '${actual:-sin respuesta}')"
  sleep 15
done

# El servicio debe estar listo (readiness) antes de la prueba de humo
curl -fsS --max-time 15 "${URL_DESPLIEGUE}/actuator/health/readiness"
echo
