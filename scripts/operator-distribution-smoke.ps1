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
    return & "$PSScriptRoot/resolve-executable-jar.ps1"
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
    "src/main/resources/application.properties",
    "src/main/resources/application-proxy-prod.properties",
    "docs/examples/proxy/application-proxy-real-backend-example.properties",
    "docs/examples/proxy/application-proxy-real-backend-weighted-example.properties",
    "docs/examples/proxy/application-proxy-real-backend-failover-example.properties",
    "docs/OPERATOR_DISTRIBUTION_SMOKE_KIT.md",
    "docs/OPERATOR_PACKAGING.md",
    "scripts/resolve-executable-jar.sh",
    "scripts/resolve-executable-jar.ps1"
)

foreach ($path in $requiredPaths) {
    Assert-PathExists -Path $path
}

$expectedJarPath = & "$PSScriptRoot/resolve-executable-jar.ps1" -ExpectedOnly

Write-Host ""
Write-Host "Package command:"
Write-Host "  mvn -B -DskipTests package"
Write-Host ""
Write-Host "Packaged jar startup:"
Write-Host "  java -jar $expectedJarPath --server.address=127.0.0.1 --server.port=$Port --spring.profiles.active=local"
Write-Host ""
Write-Host "Proxy status checks:"
Write-Host "  curl -fsS http://127.0.0.1:$Port/actuator/health"
Write-Host "  curl -fsS http://127.0.0.1:$Port/actuator/health/readiness"
Write-Host "  curl -fsS http://127.0.0.1:$Port/proxy-status.html"
Write-Host "  curl -fsS http://127.0.0.1:$Port/api/proxy/status"

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
        Invoke-UrlWithRetry -Url "http://127.0.0.1:$Port/actuator/health"
        Invoke-UrlWithRetry -Url "http://127.0.0.1:$Port/actuator/health/readiness"
        Invoke-UrlWithRetry -Url "http://127.0.0.1:$Port/proxy-status.html"
        Invoke-UrlWithRetry -Url "http://127.0.0.1:$Port/api/proxy/status"
        $proxyStatus = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/api/proxy/status" -TimeoutSec 2
        if ($proxyStatus.proxyEnabled -ne $false) {
            throw "Packaged jar smoke expected proxyEnabled=false in the explicit local profile."
        }
        Write-Host "OK: packaged jar keeps proxy forwarding disabled by default"
    } finally {
        if ($process -and -not $process.HasExited) {
            Stop-Process -Id $process.Id -Force
            Write-Host "Stopped packaged jar smoke process."
        }
    }
}
