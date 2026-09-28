#!/usr/bin/env bash
# Igual que run-perf.ps1, para Linux/macOS/Git Bash. Uso: bash perf/run-perf.sh [escenarios...]
set -euo pipefail
ESCENARIOS=("${@:-baseline carga}")
mvn -q -DskipTests package
JAR=$(ls target/orderflow-analytics-*.jar | head -1)
java -jar "$JAR" --orderflow.ui.enabled=false --orderflow.mesas=1000 > perf/results/app.log 2>&1 &
APP=$!
trap 'kill $APP' EXIT
for i in $(seq 1 60); do curl -sf http://localhost:8080/actuator/health >/dev/null && break; sleep 1; done
k6 run -e SCENARIO=baseline perf/scripts/analitica_k6.js || true
cp perf/results/analitica-baseline.md perf/results/analitica-baseline-antes.md
for e in ${ESCENARIOS[@]}; do
  k6 run -e SCENARIO="$e" perf/scripts/flujo_pedidos_k6.js || true
  curl -s http://localhost:8080/actuator/metrics/http.server.requests > "perf/results/actuator-requests-$e.json"
  curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.pending > "perf/results/actuator-hikari-pending-$e.json"
done
k6 run -e SCENARIO=carga perf/scripts/analitica_k6.js || true
echo "Pedidos al final: $(curl -s http://localhost:8080/api/pedidos | grep -o '"id"' | wc -l)" | tee perf/results/pedidos-al-final.txt
