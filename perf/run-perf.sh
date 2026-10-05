#!/usr/bin/env bash
# Igual que run-perf.ps1, para Linux/macOS/Git Bash.
# Uso: CACHE=3 MEMORIA=1g bash perf/run-perf.sh baseline carga      (CACHE=0 = sin cache, el "antes")
#      BD=archivo CACHE=3 bash perf/run-perf.sh baseline carga      (H2 en archivo: datos fuera del heap)
set -uo pipefail
CACHE=${CACHE:-3}; MEMORIA=${MEMORIA:-1g}; BD=${BD:-memoria}; ETIQUETA=${ETIQUETA:-}; SUF="cache$CACHE"
URL_BD=()
if [ "$BD" = "archivo" ]; then
  SUF="$SUF-archivo"; rm -rf perf/results/bd-perf
  URL_BD=(--spring.datasource.url="jdbc:h2:file:./perf/results/bd-perf/orderflow;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
fi
[ -n "$ETIQUETA" ] && SUF="$SUF-$ETIQUETA"
ESCENARIOS=("$@"); [ ${#ESCENARIOS[@]} -eq 0 ] && ESCENARIOS=(baseline carga)
mkdir -p perf/results
vivo() { curl -sf -m 5 http://localhost:8080/actuator/health >/dev/null; }
caida() { echo "!! Servicio caido durante: $1"; tail -20 perf/results/app.log; echo "Servicio caido durante: $1 (cache=$CACHE, memoria=$MEMORIA, base=$BD). Causa: $(grep -a OutOfMemory perf/results/app.log | tail -1)" > "perf/results/CAIDA-$SUF.txt"; exit 1; }
mvn -q -DskipTests package || exit 1
JAR=$(ls target/orderflow-analytics-*.jar | head -1)
java -Xmx"$MEMORIA" -XX:+ExitOnOutOfMemoryError -jar "$JAR" --orderflow.ui.enabled=false --orderflow.mesas=1000 \
  --orderflow.analitica.cache-segundos="$CACHE" "${URL_BD[@]}" > perf/results/app.log 2>&1 &
APP=$!
trap 'kill $APP 2>/dev/null' EXIT
for i in $(seq 1 90); do vivo && break; sleep 1; done
vivo || caida arranque
k6 run -e SCENARIO=baseline perf/scripts/analitica_k6.js
cp perf/results/analitica-baseline.md "perf/results/analitica-baseline-antes-$SUF.md"
vivo || caida "analitica baseline"
for e in "${ESCENARIOS[@]}"; do
  k6 run -e SCENARIO="$e" perf/scripts/flujo_pedidos_k6.js
  vivo || caida "flujo $e"
  cp "perf/results/flujo-pedidos-$e.md" "perf/results/flujo-pedidos-$e-$SUF.md"
  curl -s http://localhost:8080/actuator/metrics/http.server.requests > "perf/results/actuator-requests-$e-$SUF.json"
  curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.pending > "perf/results/actuator-hikari-pending-$e-$SUF.json"
done
N=$(curl -s "http://localhost:8080/api/analitica?periodo=TODO" | grep -o '"pedidosConsiderados":[0-9]*' | cut -d: -f2)
echo "Pedidos en la base: $N" | tee "perf/results/pedidos-$SUF.txt"
k6 run -e SCENARIO=carga perf/scripts/analitica_k6.js
cp perf/results/analitica-carga.md "perf/results/analitica-carga-despues-$SUF.md"
vivo || caida "analitica carga (despues de $N pedidos)"
echo "Todo termino con el servicio vivo."
