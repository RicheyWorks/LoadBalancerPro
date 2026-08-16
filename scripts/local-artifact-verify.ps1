param(
    [string]$JarPath,
    [switch]$Build
)

$ErrorActionPreference = "Stop"

function Assert-RequiredJarEntry {
    param(
        [string[]]$Entries,
        [string]$Entry
    )

    if (-not ($Entries -contains $Entry)) {
        throw "Required jar entry is missing: $Entry"
    }
    Write-Host "OK: $Entry"
}

function Assert-ForbiddenJarPattern {
    param(
        [string[]]$Entries,
        [string]$Pattern,
        [string]$Description
    )

    $matches = @($Entries | Where-Object { $_ -match $Pattern })
    if ($matches.Count -gt 0) {
        throw "Forbidden production artifact content ($Description): $($matches -join ', ')"
    }
    Write-Host "ABSENT: $Description"
}

Write-Host "LoadBalancerPro local artifact verification"
Write-Host "Release-free: no tags, releases, assets, or release workflow changes."
Write-Host "CI artifact parity: packaged-artifact-smoke contains artifact-smoke-summary.txt, artifact-sha256.txt, and jar-resource-list.txt."
Write-Host ""

if ($Build) {
    Write-Host "Running local package build:"
    & mvn -B -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed."
    }
}

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = & "$PSScriptRoot/resolve-executable-jar.ps1"
}

if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
    throw "Jar not found: $JarPath. Run with -Build or run mvn -B -DskipTests package first."
}

$hash = Get-FileHash -Algorithm SHA256 $JarPath
Write-Host "SHA-256:"
Write-Host "  $($hash.Hash)  $JarPath"
Write-Host ""

$entries = & jar tf $JarPath
if ($LASTEXITCODE -ne 0) {
    throw "jar tf failed for $JarPath"
}

Write-Host "Required jar entries:"
$requiredEntries = @(
    "META-INF/MANIFEST.MF",
    "BOOT-INF/classes/static/proxy-status.html",
    "BOOT-INF/classes/application-proxy-prod.properties",
    "BOOT-INF/classes/com/richmond423/loadbalancerpro/api/LoadBalancerApiApplication.class",
    "BOOT-INF/classes/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyService.class",
    "BOOT-INF/classes/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyStatusController.class"
)

foreach ($entry in $requiredEntries) {
    Assert-RequiredJarEntry -Entries $entries -Entry $entry
}

Write-Host ""
Write-Host "Forbidden jar entries:"
Assert-ForbiddenJarPattern -Entries $entries -Pattern '^BOOT-INF/classes/com/richmond423/loadbalancerpro/(cli|demo|gui|lab)/' -Description "lab, demo, GUI, or CLI application classes"
Assert-ForbiddenJarPattern -Entries $entries -Pattern '^BOOT-INF/classes/com/richmond423/loadbalancerpro/api/(AllocatorController|DecisionExplorer|EnterpriseLab|Evidence|Lase|Remediation|RoutingController|ScenarioReplay)' -Description "simulation API controllers and services"
Assert-ForbiddenJarPattern -Entries $entries -Pattern '^BOOT-INF/classes/com/richmond423/loadbalancerpro/core/(CloudManager|LoadBalancer|ServerMonitor)' -Description "simulation, cloud-manager, or synthetic-monitor core"
Assert-ForbiddenJarPattern -Entries $entries -Pattern '^BOOT-INF/classes/static/(adaptive-routing|ci-evidence|decision-explorer|enterprise-lab|evidence-|index\.html|load-balancing|operator-evidence|routing-demo)' -Description "simulation and cockpit web pages"
Assert-ForbiddenJarPattern -Entries $entries -Pattern '^BOOT-INF/lib/(autoscaling|cloudwatch|ec2|aws-|sdk-core|reactor-core|gson)-' -Description "lab-only cloud, reactive, or JSON libraries"
Assert-ForbiddenJarPattern -Entries $entries -Pattern '^BOOT-INF/classes/.*(Test|Tests)\.class$' -Description "test classes"
Assert-ForbiddenJarPattern -Entries $entries -Pattern '\.(exe|dll|msi|dmg|pkg|deb|rpm|appimage)$' -Description "unexpected native installers or executables"
Assert-ForbiddenJarPattern -Entries $entries -Pattern '\.(pem|p12|pfx|jks|keystore|key)$' -Description "embedded key, certificate, or trust material"

Write-Host ""
Write-Host "Packaged jar startup:"
Write-Host "  java -jar $JarPath --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=local"
Write-Host ""
Write-Host "Status and static page checks:"
Write-Host "  curl -fsS http://127.0.0.1:8080/actuator/health"
Write-Host "  curl -fsS http://127.0.0.1:8080/proxy-status.html"
Write-Host "  curl -fsS http://127.0.0.1:8080/api/proxy/status"
Write-Host ""
Write-Host "Optional Lab Tools artifact:"
Write-Host "  mvn -B -P lab -DskipTests package"
Write-Host "  ./scripts/resolve-executable-jar.ps1 -Lab"
Write-Host ""
Write-Host "GitHub Actions artifact:"
Write-Host "  packaged-artifact-smoke"
Write-Host ""
Write-Host "Do not commit generated jars, checksums, manifests, or smoke output."
