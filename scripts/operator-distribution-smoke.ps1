param(
    [switch]$Package,
    [switch]$RunJarSmoke,
    [int]$Port = 18080
)

$ErrorActionPreference = "Stop"

function Assert-PathExists {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "Required smoke asset is missing: $Path"
    }
    Write-Host "OK: $Path"
}

function Find-ExecutableJar {
    $jar = Get-ChildItem -Path "target" -Filter "LoadBalancerPro-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "(-sources|-javadoc|-tests|\\.original)\\.jar$" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $jar) {
        throw "Executable jar not found under target/. Run with -Package or run mvn -B -DskipTests package first."
    }
    return $jar.FullName
}

function Invoke-UrlWithRetry {
    param(
        [string]$Url,
        [int]$Attempts = 30
    )

    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        try {
            Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 2 | Out-Null
            Write-Host "OK: $Url"
            return
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    throw "Timed out waiting for $Url"
}

Write-Host "LoadBalancerPro release-free operator distribution smoke kit"
Write-Host "No tag, release, or asset creation is performed by this script."
Write-Host ""

$requiredPaths = @(
    "pom.xml",
    "src/main/resources/static/proxy-status.html",
    "src/main/resources/static/load-balancing-cockpit.html",
    "src/main/resources/application.properties",
    "src/main/resources/application-proxy-demo-round-robin.properties",
    "src/main/resources/application-proxy-demo-weighted-round-robin.properties",
    "src/main/resources/application-proxy-demo-failover.properties",
    "docs/examples/proxy/application-proxy-real-backend-example.properties",
    "docs/examples/proxy/application-proxy-real-backend-weighted-example.properties",
    "docs/examples/proxy/application-proxy-real-backend-failover-example.properties",
    "docs/OPERATOR_DISTRIBUTION_SMOKE_KIT.md",
    "docs/OPERATOR_PACKAGING.md",
    "docs/PROXY_DEMO_FIXTURE_LAUNCHER.md",
    "docs/PROXY_DEMO_STACK.md"
)

foreach ($path in $requiredPaths) {
    Assert-PathExists -Path $path
}

Write-Host ""
Write-Host "Package command:"
Write-Host "  mvn -B -DskipTests package"
Write-Host ""
Write-Host "Maven exec fixture launcher:"
Write-Host "  mvn -q -DskipTests compile exec:java `"-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher`" `"-Dexec.args=--mode round-robin`""
Write-Host ""
Write-Host "Packaged jar startup:"
Write-Host "  java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=$Port --spring.profiles.active=local"
Write-Host ""
Write-Host "Proxy status checks:"
Write-Host "  curl -fsS http://127.0.0.1:$Port/api/health"
Write-Host "  curl -fsS http://127.0.0.1:$Port/proxy-status.html"
Write-Host "  curl -fsS http://127.0.0.1:$Port/api/proxy/status"
Write-Host ""
Write-Host "Demo profiles:"
Write-Host "  proxy-demo-round-robin"
Write-Host "  proxy-demo-weighted-round-robin"
Write-Host "  proxy-demo-failover"

if ($Package) {
    Write-Host ""
    Write-Host "Running package smoke:"
    & mvn -B -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed."
    }
    $jarPath = Find-ExecutableJar
    Write-Host "OK: packaged jar $jarPath"
} elseif ($RunJarSmoke) {
    $jarPath = Find-ExecutableJar
}

if ($RunJarSmoke) {
    if (-not $jarPath) {
        $jarPath = Find-ExecutableJar
    }
    Write-Host ""
    Write-Host "Running packaged jar HTTP smoke on 127.0.0.1:$Port"
    $arguments = @(
        "-jar", $jarPath,
        "--server.address=127.0.0.1",
        "--server.port=$Port",
        "--spring.profiles.active=local"
    )
    $process = Start-Process -FilePath "java" -ArgumentList $arguments -PassThru -WindowStyle Hidden
    try {
        Invoke-UrlWithRetry -Url "http://127.0.0.1:$Port/api/health"
        Invoke-UrlWithRetry -Url "http://127.0.0.1:$Port/proxy-status.html"
        Invoke-UrlWithRetry -Url "http://127.0.0.1:$Port/api/proxy/status"
    } finally {
        if ($process -and -not $process.HasExited) {
            Stop-Process -Id $process.Id -Force
            Write-Host "Stopped packaged jar smoke process."
        }
    }
}
