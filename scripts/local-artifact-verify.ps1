param(
    [string]$JarPath = "target/LoadBalancerPro-2.5.0.jar",
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

if (-not (Test-Path $JarPath)) {
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
    "BOOT-INF/classes/static/load-balancing-cockpit.html",
    "BOOT-INF/classes/application-proxy-demo-round-robin.properties",
    "BOOT-INF/classes/application-proxy-demo-weighted-round-robin.properties",
    "BOOT-INF/classes/application-proxy-demo-failover.properties",
    "BOOT-INF/classes/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.class"
)

foreach ($entry in $requiredEntries) {
    Assert-RequiredJarEntry -Entries $entries -Entry $entry
}

Write-Host ""
Write-Host "Packaged jar startup:"
Write-Host "  java -jar $JarPath --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=local"
Write-Host ""
Write-Host "Status and static page checks:"
Write-Host "  curl -fsS http://127.0.0.1:8080/api/health"
Write-Host "  curl -fsS http://127.0.0.1:8080/proxy-status.html"
Write-Host "  curl -fsS http://127.0.0.1:8080/load-balancing-cockpit.html"
Write-Host "  curl -fsS http://127.0.0.1:8080/api/proxy/status"
Write-Host ""
Write-Host "Maven exec fixture launcher:"
Write-Host "  mvn -q -DskipTests compile exec:java `"-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher`" `"-Dexec.args=--mode round-robin`""
Write-Host ""
Write-Host "GitHub Actions artifact:"
Write-Host "  packaged-artifact-smoke"
Write-Host ""
Write-Host "Do not commit generated jars, checksums, manifests, or smoke output."
