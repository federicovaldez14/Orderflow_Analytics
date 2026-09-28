# Ejecuta las pruebas de carga de punta a punta en Windows (PowerShell).
# Uso (desde la raiz del repositorio):
#   powershell -ExecutionPolicy Bypass -File perf\run-perf.ps1
#   powershell -ExecutionPolicy Bypass -File perf\run-perf.ps1 -Escenarios baseline,carga,pico
# Requisitos: Java 17+, Maven y k6 (winget install k6 --source winget)

param(
    [string[]]$Escenarios = @("baseline", "carga")
)
$ErrorActionPreference = "Stop"

Write-Host "== 1. Compilando el jar (sin pruebas) =="
mvn -q -DskipTests package

Write-Host "== 2. Levantando el servicio sin ventana y con 1000 mesas =="
$jar = Get-ChildItem target\orderflow-analytics-*.jar | Select-Object -First 1
$app = Start-Process java -ArgumentList "-jar", $jar.FullName, "--orderflow.ui.enabled=false", "--orderflow.mesas=1000" `
    -PassThru -RedirectStandardOutput perf\results\app.log -RedirectStandardError perf\results\app-err.log
try {
    for ($i = 0; $i -lt 60; $i++) {
        try { if ((Invoke-RestMethod http://localhost:8080/actuator/health).status -eq "UP") { break } } catch { }
        Start-Sleep -Seconds 1
    }
    Write-Host "Servicio arriba."

    Write-Host "== 3. Analitica ANTES de cargar pedidos (solo historial de demo) =="
    k6 run -e SCENARIO=baseline perf\scripts\analitica_k6.js
    Copy-Item perf\results\analitica-baseline.md perf\results\analitica-baseline-antes.md -Force

    foreach ($e in $Escenarios) {
        Write-Host "== 4. Flujo de pedidos: escenario $e =="
        k6 run -e SCENARIO=$e perf\scripts\flujo_pedidos_k6.js
        Invoke-RestMethod "http://localhost:8080/actuator/metrics/http.server.requests" |
            ConvertTo-Json -Depth 6 | Out-File "perf\results\actuator-requests-$e.json"
        Invoke-RestMethod "http://localhost:8080/actuator/metrics/hikaricp.connections.pending" |
            ConvertTo-Json -Depth 6 | Out-File "perf\results\actuator-hikari-pending-$e.json"
    }

    Write-Host "== 5. Analitica DESPUES (con miles de pedidos mas) =="
    k6 run -e SCENARIO=carga perf\scripts\analitica_k6.js
    $n = (Invoke-RestMethod http://localhost:8080/api/pedidos).Count
    "Pedidos en la base al final: $n" | Out-File perf\results\pedidos-al-final.txt
    Write-Host "Pedidos en la base al final: $n"
}
finally {
    Write-Host "== 6. Deteniendo el servicio =="
    Stop-Process -Id $app.Id -Force
}
Write-Host "Resultados en perf\results (archivos .md listos para pegar en docs\pruebas.md)"
