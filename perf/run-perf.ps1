# Ejecuta las pruebas de carga de punta a punta en Windows (PowerShell).
# Uso (desde la raiz del repositorio):
#   powershell -ExecutionPolicy Bypass -File perf\run-perf.ps1
#   powershell -ExecutionPolicy Bypass -File perf\run-perf.ps1 -Escenarios baseline,carga,pico
#   powershell -ExecutionPolicy Bypass -File perf\run-perf.ps1 -CacheAnalitica 0   # "antes" de la mitigacion
#   powershell -ExecutionPolicy Bypass -File perf\run-perf.ps1 -BaseDatos archivo  # H2 en archivo (datos fuera del heap)
#   powershell -ExecutionPolicy Bypass -File perf\run-perf.ps1 -Escenarios baseline -Etiqueta volumen-medio
# Requisitos: Java 17+, Maven y k6 (winget install k6 --source winget)

param(
    [string[]]$Escenarios = @("baseline", "carga"),
    # Vigencia en segundos de la cache del panel de analitica (0 = sin cache).
    [int]$CacheAnalitica = 3,
    # Memoria maxima de la JVM: fija para que las corridas sean comparables entre maquinas.
    [string]$Memoria = "1g",
    # memoria: H2 en memoria (por defecto, comparte el heap con la app); archivo: H2 en disco.
    # Solo cambia la configuracion del adaptador de persistencia (ADR-002), no el codigo.
    [ValidateSet("memoria", "archivo")]
    [string]$BaseDatos = "memoria",
    # Texto que se agrega al sufijo de los resultados para no sobrescribir otra corrida (ej. "volumen-medio").
    [string]$Etiqueta = ""
)
$ErrorActionPreference = "Stop"
$sufijo = "cache$CacheAnalitica"
if ($BaseDatos -eq "archivo") { $sufijo = "$sufijo-archivo" }
if ($Etiqueta) { $sufijo = "$sufijo-$Etiqueta" }
New-Item -ItemType Directory -Force perf\results | Out-Null

# En Windows "java" suele ser un lanzador (javapath) que arranca el java.exe real: detener solo el
# PID devuelto por Start-Process deja vivo el servidor y ocupa el puerto de la siguiente corrida.
function Detener-Puerto8080 {
    Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}

function Esta-Vivo {
    try { return (Invoke-RestMethod http://localhost:8080/actuator/health -TimeoutSec 5).status -eq "UP" }
    catch { return $false }
}

function Reportar-Caida([string]$paso) {
    Write-Host ""
    Write-Host "!! El servicio dejo de responder durante: $paso" -ForegroundColor Red
    Write-Host "!! Ultimas lineas de perf\results\app-err.log y app.log:" -ForegroundColor Red
    Get-Content perf\results\app-err.log -Tail 20 -ErrorAction SilentlyContinue
    Get-Content perf\results\app.log -Tail 20 -ErrorAction SilentlyContinue
    # La JVM escribe el OutOfMemoryError a veces en stdout (app.log) y a veces en stderr (app-err.log).
    $causa = (Select-String -Path perf\results\app.log, perf\results\app-err.log -Pattern "OutOfMemoryError" -ErrorAction SilentlyContinue |
        Select-Object -Last 1).Line
    if (-not $causa) { $causa = "sin OutOfMemoryError en los logs: el servicio quedo saturado (no respondio /actuator/health en 5 s)" }
    "Servicio caido durante: $paso (cache=$CacheAnalitica s, memoria=$Memoria, base=$BaseDatos). Causa: $causa" |
        Out-File "perf\results\CAIDA-$sufijo.txt" -Encoding utf8
}

if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
    throw "El puerto 8080 ya esta en uso: detenga la aplicacion que lo ocupa antes de medir"
}

Write-Host "== 1. Compilando el jar (sin pruebas) =="
mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion" }

Write-Host "== 2. Levantando el servicio (sin ventana, 1000 mesas, cache analitica $CacheAnalitica s, memoria $Memoria, base $BaseDatos) =="
$jar = Get-ChildItem target\orderflow-analytics-*.jar | Select-Object -First 1
$argumentos = @("-Xmx$Memoria", "-XX:+ExitOnOutOfMemoryError", "-jar", $jar.FullName,
    "--orderflow.ui.enabled=false", "--orderflow.mesas=1000", "--orderflow.analitica.cache-segundos=$CacheAnalitica")
if ($BaseDatos -eq "archivo") {
    Remove-Item perf\results\bd-perf -Recurse -Force -ErrorAction SilentlyContinue   # cada corrida empieza limpia
    $argumentos += "--spring.datasource.url=jdbc:h2:file:./perf/results/bd-perf/orderflow;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"
}
$app = Start-Process java -ArgumentList $argumentos -PassThru `
    -RedirectStandardOutput perf\results\app.log -RedirectStandardError perf\results\app-err.log
try {
    for ($i = 0; $i -lt 90 -and -not (Esta-Vivo); $i++) { Start-Sleep -Seconds 1 }
    if (-not (Esta-Vivo)) { Reportar-Caida "arranque"; return }
    Write-Host "Servicio arriba."

    Write-Host "== 3. Analitica ANTES de cargar pedidos (solo historial de demo) =="
    k6 run -e SCENARIO=baseline perf\scripts\analitica_k6.js
    Copy-Item perf\results\analitica-baseline.md "perf\results\analitica-baseline-antes-$sufijo.md" -Force
    if (-not (Esta-Vivo)) { Reportar-Caida "analitica baseline"; return }

    foreach ($e in $Escenarios) {
        Write-Host "== 4. Flujo de pedidos: escenario $e =="
        k6 run -e SCENARIO=$e perf\scripts\flujo_pedidos_k6.js
        if (-not (Esta-Vivo)) { Reportar-Caida "flujo $e"; return }
        Copy-Item "perf\results\flujo-pedidos-$e.md" "perf\results\flujo-pedidos-$e-$sufijo.md" -Force
        Invoke-RestMethod "http://localhost:8080/actuator/metrics/http.server.requests" |
            ConvertTo-Json -Depth 6 | Out-File "perf\results\actuator-requests-$e-$sufijo.json" -Encoding utf8
        Invoke-RestMethod "http://localhost:8080/actuator/metrics/hikaricp.connections.pending" |
            ConvertTo-Json -Depth 6 | Out-File "perf\results\actuator-hikari-pending-$e-$sufijo.json" -Encoding utf8
    }

    $n = (Invoke-RestMethod http://localhost:8080/api/analitica?periodo=TODO).pedidosConsiderados
    "Pedidos en la base antes de la analitica bajo carga: $n" | Out-File "perf\results\pedidos-$sufijo.txt" -Encoding utf8
    Write-Host "Pedidos en la base: $n"

    Write-Host "== 5. Analitica DESPUES (con $n pedidos) =="
    k6 run -e SCENARIO=carga perf\scripts\analitica_k6.js
    Copy-Item perf\results\analitica-carga.md "perf\results\analitica-carga-despues-$sufijo.md" -Force
    if (-not (Esta-Vivo)) { Reportar-Caida "analitica carga (despues de $n pedidos)"; return }
    Invoke-RestMethod "http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap" |
        ConvertTo-Json -Depth 6 | Out-File "perf\results\actuator-heap-$sufijo.json" -Encoding utf8
    Write-Host "Todo termino con el servicio vivo." -ForegroundColor Green
}
finally {
    Write-Host "== 6. Deteniendo el servicio =="
    Stop-Process -Id $app.Id -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Detener-Puerto8080
}
Write-Host "Resultados en perf\results (archivos -$sufijo.md listos para pegar en docs\pruebas.md)"
