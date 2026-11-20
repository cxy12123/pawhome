# PawHome one-click launcher (single window mode)
# Called by 2-start.bat. Services run hidden in background; logs under run\logs
# Paths and credentials are parsed from config.bat, so no env inheritance is needed.

$ErrorActionPreference = 'Continue'
$scriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$root       = Split-Path -Parent $scriptDir
$logDir     = Join-Path $scriptDir 'logs'
$configPath = Join-Path $scriptDir 'config.bat'

function Line($text, $color = 'Gray') { Write-Host $text -ForegroundColor $color }

function Get-Cfg($key) {
    if (-not (Test-Path $configPath)) { return $null }
    $raw = Get-Content $configPath -Raw -Encoding Default
    $m = [regex]::Match($raw, 'set\s+"' + [regex]::Escape($key) + '=([^"\r\n]*)"')
    if ($m.Success) { return $m.Groups[1].Value.Trim() }
    $m2 = [regex]::Match($raw, 'set\s+' + [regex]::Escape($key) + '=([^\r\n]*)')
    if ($m2.Success) { return $m2.Groups[1].Value.Trim() }
    return $null
}

function Test-Port($port, $timeoutMs = 500) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $iar = $client.BeginConnect('127.0.0.1', $port, $null, $null)
        if ($iar.AsyncWaitHandle.WaitOne($timeoutMs, $false) -and $client.Connected) { return $true }
        return $false
    } catch { return $false } finally { $client.Close() }
}

# If both HTTPS_PROXY and https_proxy exist, the process-start API throws on a
# duplicate env key and no process starts at all. All calls here are loopback.
# Do NOT enumerate with Get-ChildItem env: - it throws the same exception.
foreach ($proxyName in @('http_proxy','https_proxy','all_proxy','no_proxy',
                         'HTTP_PROXY','HTTPS_PROXY','ALL_PROXY','NO_PROXY')) {
    try { [System.Environment]::SetEnvironmentVariable($proxyName, $null, 'Process') } catch { }
}

# ---------- read config.bat ----------
$jdkDir      = Get-Cfg 'JDK17'
$redisDir    = Get-Cfg 'REDIS_DIR'
$nacosDir    = Get-Cfg 'NACOS_DIR'
$sentinelDir = Get-Cfg 'SENTINEL_DIR'
$mysqlUser   = Get-Cfg 'MYSQL_USERNAME'
$mysqlPwd    = Get-Cfg 'MYSQL_PASSWORD'
$nacosUser   = Get-Cfg 'NACOS_USERNAME'
$nacosPwd    = Get-Cfg 'NACOS_PASSWORD'

if (-not $jdkDir)      { $jdkDir      = $env:JAVA_HOME }
if (-not $redisDir)    { $redisDir    = $env:REDIS_DIR }
if (-not $nacosDir)    { $nacosDir    = $env:NACOS_DIR }
if (-not $sentinelDir) { $sentinelDir = $env:SENTINEL_DIR }

$javaExe = if ($jdkDir) { Join-Path $jdkDir 'bin\java.exe' } else { $null }

# ---------- preflight ----------
$problems = @()

if (-not $jdkDir -or -not (Test-Path $javaExe)) {
    $problems += "JDK 17 not found. Set JDK17 in config.bat (current: '$jdkDir')"
} else {
    # java -version writes to stderr; capture through cmd so the text is not lost
    $verText = ''
    try { $verText = cmd /c "`"$javaExe`" -version 2>&1" | Out-String } catch { }
    if ($verText -notmatch 'version "17') {
        $first = if ($verText.Trim()) { ($verText -split "`n")[0].Trim() } else { 'no output' }
        $problems += "Wrong JDK: $javaExe -> $first (Java 17 required)"
    }
}

if (-not $redisDir -or -not (Test-Path (Join-Path $redisDir 'redis-server.exe'))) {
    $problems += "Redis not found. Set REDIS_DIR in config.bat (current: '$redisDir')"
}
if (-not $nacosDir -or -not (Test-Path (Join-Path $nacosDir 'bin\startup.cmd'))) {
    $problems += "Nacos not found. Set NACOS_DIR in config.bat (current: '$nacosDir')"
}

$services = @(
    @{ Name = 'auth';      Jar = 'pawhome-auth.jar';          Port = 8081 },
    @{ Name = 'animal';    Jar = 'pawhome-animal.jar';        Port = 8082 },
    @{ Name = 'adoption';  Jar = 'pawhome-adoption.jar';      Port = 8083 },
    @{ Name = 'supply';    Jar = 'pawhome-supply.jar';        Port = 8084 },
    @{ Name = 'donation';  Jar = 'pawhome-donation.jar';      Port = 8085 },
    @{ Name = 'volunteer'; Jar = 'pawhome-volunteer.jar';     Port = 8086 },
    @{ Name = 'notify';    Jar = 'pawhome-notify.jar';        Port = 8087 },
    @{ Name = 'gateway';   Jar = 'pawhome-gateway-1.0.0.jar'; Port = 8080 }
)

foreach ($s in $services) {
    $jarPath = Join-Path $root ("pawhome-{0}\target\{1}" -f $s.Name, $s.Jar)
    if (-not (Test-Path $jarPath)) { $problems += "Missing jar: $jarPath (run 1-build.bat)" }
}

if ($problems.Count -gt 0) {
    Line ""
    Line "============================================================" 'Red'
    Line "  START ABORTED - fix these first" 'Red'
    Line "============================================================" 'Red'
    foreach ($p in $problems) { Line "  - $p" 'Yellow' }
    Line ""
    Line "  Config file: $configPath" 'Gray'
    Line ""
    Read-Host "Press Enter to exit"
    exit 1
}

if (Test-Port 8080) {
    Line ""
    Line "[WARN] Port 8080 already in use - PawHome may already be running." 'Yellow'
    Line "       Run 3-stop.bat first for a clean restart." 'Yellow'
    Line ""
    Read-Host "Press Enter to exit"
    exit 1
}

if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }
Get-ChildItem $logDir -Filter 'pawhome-*.log' -ErrorAction SilentlyContinue |
    Remove-Item -Force -ErrorAction SilentlyContinue

$env:JAVA_HOME      = $jdkDir
$env:MYSQL_USERNAME = $mysqlUser
$env:MYSQL_PASSWORD = $mysqlPwd
$env:NACOS_USERNAME = $nacosUser
$env:NACOS_PASSWORD = $nacosPwd

Line ""
Line "============================================================" 'DarkCyan'
Line "  PawHome - single window mode" 'White'
Line "  Java : $javaExe" 'Gray'
Line "  Logs : $logDir" 'Gray'
Line ""
Line "  13 processes run in background." 'Gray'
Line "  Close this window any time - services keep running." 'Gray'
Line "  To stop everything use 3-stop.bat" 'Gray'
Line "============================================================" 'DarkCyan'
Line ""

# ---------- middleware ----------
Line "[1/6] Redis ..." 'White'
try {
    $redisProc = Start-Process -FilePath (Join-Path $redisDir 'redis-server.exe') `
        -ArgumentList 'redis.windows.conf' -WorkingDirectory $redisDir -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logDir 'pawhome-redis.log') `
        -RedirectStandardError  (Join-Path $logDir 'pawhome-redis.err.log')
    Start-Sleep -Seconds 2
    if ($redisProc.HasExited) { Line "      FAILED (exit $($redisProc.ExitCode)) - see pawhome-redis.err.log" 'Red' }
    else { Line "      ok  pid $($redisProc.Id)" 'Green' }
} catch {
    Line "      LAUNCH FAILED: $($_.Exception.Message)" 'Red'
}

Line "[2/6] Nacos ..." 'White'
$nacosProc = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'startup.cmd -m standalone' `
    -WorkingDirectory (Join-Path $nacosDir 'bin') -WindowStyle Minimized -PassThru
Start-Sleep -Seconds 2
if ($nacosProc.HasExited) { Line "      FAILED (exit $($nacosProc.ExitCode))" 'Red' }
else { Line "      ok  minimised window" 'Green' }

Line ""
Line "Waiting for Nacos (HTTP 8848 + gRPC 9848, max 75s) ..." 'Gray'
for ($i = 0; $i -lt 75; $i++) {
    Start-Sleep -Seconds 1
    if ((Test-Port 8848) -and (Test-Port 9848)) { break }
}
if ((Test-Port 8848) -and (Test-Port 9848)) {
    Line "Nacos ready." 'Green'
} elseif (Test-Port 8848) {
    Line "[WARN] Nacos HTTP 8848 is up but gRPC 9848 is not - registration will fail" 'Yellow'
} else {
    Line "[WARN] Nacos not listening - services will fail to register" 'Yellow'
}

# ---------- Eureka registry (textbook 11.4.2 / 11.5.2) ----------
Line "[3/6] Eureka Server ..." 'White'
$eurekaJar = Join-Path $root 'pawhome-eureka\target\pawhome-eureka-1.0.0.jar'
if (Test-Path $eurekaJar) {
    try {
        $eurekaProc = Start-Process -FilePath $javaExe -ArgumentList '-jar', 'pawhome-eureka-1.0.0.jar' `
            -WorkingDirectory (Join-Path $root 'pawhome-eureka\target') -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $logDir 'pawhome-eureka.log') `
            -RedirectStandardError  (Join-Path $logDir 'pawhome-eureka.err.log')
        Start-Sleep -Seconds 2
        if ($eurekaProc.HasExited) { Line "      FAILED - see pawhome-eureka.err.log" 'Red' }
        else { Line "      ok  pid $($eurekaProc.Id)" 'Green' }
    } catch {
        Line "      LAUNCH FAILED: $($_.Exception.Message)" 'Red'
    }
} else {
    Line "      skipped - jar not found, run 1-build.bat" 'DarkGray'
}

Line "      waiting for Eureka 8761 (max 60s) ..." 'Gray'
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    if (Test-Port 8761) { break }
}
if (Test-Port 8761) { Line "      Eureka ready." 'Green' }
else { Line "      [WARN] Eureka not up - dashboard will stay empty" 'Yellow' }

# ---------- Config server (textbook 11.4.6 / 11.5.7) ----------
Line "[4/6] Config Server ..." 'White'
$configJar = Join-Path $root 'pawhome-config\target\pawhome-config-1.0.0.jar'
if (Test-Path $configJar) {
    try {
        $cfgProc = Start-Process -FilePath $javaExe -ArgumentList '-jar', 'pawhome-config-1.0.0.jar' `
            -WorkingDirectory (Join-Path $root 'pawhome-config\target') -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $logDir 'pawhome-config.log') `
            -RedirectStandardError  (Join-Path $logDir 'pawhome-config.err.log')
        Start-Sleep -Seconds 2
        if ($cfgProc.HasExited) { Line "      FAILED - see pawhome-config.err.log" 'Red' }
        else { Line "      ok  pid $($cfgProc.Id)" 'Green' }
    } catch {
        Line "      LAUNCH FAILED: $($_.Exception.Message)" 'Red'
    }
} else {
    Line "      skipped - jar not found, run 1-build.bat" 'DarkGray'
}

# ---------- Sentinel ----------
Line "[5/6] Sentinel Dashboard ..." 'White'
$sentinelJar = if ($sentinelDir) { Join-Path $sentinelDir 'sentinel-dashboard-1.8.7.jar' } else { $null }
if ($sentinelJar -and (Test-Path $sentinelJar)) {
    try {
        $senProc = Start-Process -FilePath $javaExe `
            -ArgumentList '-Dserver.port=8858', '-jar', 'sentinel-dashboard-1.8.7.jar' `
            -WorkingDirectory $sentinelDir -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $logDir 'pawhome-sentinel.log') `
            -RedirectStandardError  (Join-Path $logDir 'pawhome-sentinel.err.log')
        Start-Sleep -Seconds 2
        if ($senProc.HasExited) { Line "      FAILED - see pawhome-sentinel.err.log" 'Red' }
        else { Line "      ok  pid $($senProc.Id)" 'Green' }
    } catch {
        Line "      LAUNCH FAILED: $($_.Exception.Message)" 'Red'
    }
} else {
    Line "      skipped (sentinel jar not found, optional)" 'DarkGray'
}

# ---------- services ----------
Line ""
Line "[6/6] Starting 8 microservices ..." 'White'
foreach ($s in $services) {
    $svcDir = Join-Path $root ("pawhome-{0}\target" -f $s.Name)
    try {
        $proc = Start-Process -FilePath $javaExe -ArgumentList '-jar', $s.Jar `
            -WorkingDirectory $svcDir -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $logDir ("pawhome-{0}.log" -f $s.Name)) `
            -RedirectStandardError  (Join-Path $logDir ("pawhome-{0}.err.log" -f $s.Name))
        Start-Sleep -Milliseconds 900
        if ($proc.HasExited) {
            Line ("      pawhome-{0,-10} :{1}   EXITED (code {2}) - see err log" -f $s.Name, $s.Port, $proc.ExitCode) 'Red'
        } else {
            Line ("      pawhome-{0,-10} :{1}   running (pid {2})" -f $s.Name, $s.Port, $proc.Id) 'Gray'
        }
    } catch {
        Line ("      pawhome-{0,-10} :{1}   LAUNCH FAILED: {2}" -f $s.Name, $s.Port, $_.Exception.Message) 'Red'
    }
}

Line ""
Line "Waiting for gateway on 8080 (max 120s) ..." 'Gray'
$waited = 0
while ($waited -lt 120) {
    Start-Sleep -Seconds 3
    $waited += 3
    if (Test-Port 8080) { break }
}

# Gateway being up does not mean the others are ready: Spring Boot still needs
# tens of seconds. Keep polling until all 8 ports are open, or give up.
Line ""
Line "Waiting for all services to open their ports (max 90s) ..." 'Gray'
$allWaited = 0
while ($allWaited -lt 90) {
    $allUp = $true
    foreach ($s in $services) {
        if (-not (Test-Port $s.Port)) { $allUp = $false; break }
    }
    if ($allUp) { break }
    Start-Sleep -Seconds 3
    $allWaited += 3
}

# ---------- status ----------
Line ""
Line "============================================================" 'DarkCyan'
if (Test-Port 8080) { Line "  READY - gateway up after $waited s" 'Green' }
else { Line "  NOT READY - gateway did not open 8080 in 120s" 'Red' }
Line ""
Line "  Service status:" 'White'

$rows = @()
foreach ($s in $services) { $rows += @{ Label = ("pawhome-{0}" -f $s.Name); Port = $s.Port } }
$rows += @{ Label = 'redis';    Port = 6379 }
$rows += @{ Label = 'nacos';    Port = 8848 }
$rows += @{ Label = 'eureka';   Port = 8761 }
$rows += @{ Label = 'config';   Port = 8888 }
$rows += @{ Label = 'sentinel'; Port = 8858 }

foreach ($r in $rows) {
    $up = Test-Port $r.Port
    $tag = if ($up) { 'UP  ' } else { 'DOWN' }
    Line ("    {0}  {1,-20} :{2}" -f $tag, $r.Label, $r.Port) $(if ($up) { 'Green' } else { 'Red' })
}

Line ""
Line "  Frontend   http://localhost:8080/          admin / 123456" 'Gray'
Line "  Registry   http://localhost:8848/nacos     nacos / nacos" 'Gray'
Line "  Eureka     http://localhost:8761            (dashboard)" 'Gray'
Line "  Config     http://localhost:8888/pawhome-supply/default" 'Gray'
Line "  Sentinel   http://localhost:8858           sentinel / sentinel" 'Gray'
Line ""
Line "  Stop    : 3-stop.bat" 'Gray'
Line "  Logs    : 5-view-log.bat  (files under run\logs)" 'Gray'
Line "============================================================" 'DarkCyan'
Line ""

if (Test-Port 8080) { Start-Process 'http://localhost:8080/' }

Read-Host "Press Enter to exit"
