param(
    [switch]$DryRun,
    [switch]$Package,
    [ValidateSet("completion", "rollback", "all")]
    [string]$Suite = "all",
    [string]$OutputDir = "target/enterprise-lab-experiment-proof-smoke"
)

$ErrorActionPreference = "Stop"

function Resolve-RepoPath {
    param([string]$Path)
    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $Path))
}

function Assert-OutputUnderTarget {
    param([string]$Path)
    $targetRoot = Resolve-RepoPath "target"
    $resolvedOutput = Resolve-RepoPath $Path
    if (-not $resolvedOutput.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Enterprise Lab experiment proof output must stay under target/. Requested: $Path"
    }
    return $resolvedOutput
}

function Find-ExecutableJar {
    $jar = Get-ChildItem -Path "target" -Filter "LoadBalancerPro-*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "(-sources|-javadoc|-tests|\.original)\.jar$" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $jar) {
        return $null
    }
    return $jar.FullName
}

function Assert-NoSecretValues {
    param([string]$Text)
    $patterns = @(
        '(?i)bearer\s+[a-z0-9._~+/-]{12,}',
        '(?i)x-api-key\s*[:=]\s*[a-z0-9._~+/-]{8,}',
        '(?i)(password|secret|credential|token)\s*[:=]\s*[a-z0-9._~+/-]{8,}',
        'CHANGE_ME_LOCAL_API_KEY'
    )
    foreach ($pattern in $patterns) {
        if ($Text -match $pattern) {
            throw "Refusing Enterprise Lab experiment proof output that looks secret-bearing."
        }
    }
}

$resolvedOutputDir = Assert-OutputUnderTarget -Path $OutputDir

if ($DryRun) {
    Write-Host "Enterprise Lab experiment proof dry run."
    Write-Host "Suite: $Suite"
    Write-Host "Output directory: $OutputDir"
    Write-Host "Planned command: java -jar target/LoadBalancerPro-*.jar --enterprise-lab-experiment-proof=$Suite --enterprise-lab-experiment-output=$OutputDir"
    Write-Host "Safety: foreground bounded literal-loopback proof only; no API server, external target, cloud, tenant, production routing, release, container publication, or registry action."
    exit 0
}

$jarPath = Find-ExecutableJar
if ($Package -or $null -eq $jarPath) {
    & mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "mvn package failed with exit code $LASTEXITCODE"
    }
    $jarPath = Find-ExecutableJar
}
if ($null -eq $jarPath) {
    throw "Executable LoadBalancerPro jar was not found under target/."
}

New-Item -ItemType Directory -Force -Path $resolvedOutputDir | Out-Null
$output = & java -jar $jarPath "--enterprise-lab-experiment-proof=$Suite" "--enterprise-lab-experiment-output=$OutputDir" 2>&1
$exitCode = $LASTEXITCODE
$text = ($output -join "`n")
if ($exitCode -ne 0) {
    throw "Enterprise Lab experiment proof CLI failed with exit code $exitCode`n$text"
}
Assert-NoSecretValues -Text $text

$expectedFiles = @(
    "enterprise-lab-experiment-proof.json",
    "enterprise-lab-experiment-proof-summary.md",
    "enterprise-lab-experiment-proof-metadata.json"
)
foreach ($file in $expectedFiles) {
    $path = Join-Path $resolvedOutputDir $file
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Expected Enterprise Lab experiment proof file missing: $path"
    }
    Assert-NoSecretValues -Text (Get-Content -LiteralPath $path -Raw)
}

$reportPath = Join-Path $resolvedOutputDir "enterprise-lab-experiment-proof.json"
$reportText = Get-Content -LiteralPath $reportPath -Raw
if ($reportText -match '(?i)https?://(?!127\.0\.0\.1)') {
    throw "Experiment proof evidence contains a non-loopback network target."
}
$report = $reportText | ConvertFrom-Json
if (-not $report.allPassed) {
    throw "Experiment proof report did not pass every bounded scenario."
}
if ($report.requestedSuite -ne $Suite) {
    throw "Experiment proof report suite does not match the requested suite."
}
if ($report.totalActualRequests -lt 1) {
    throw "Experiment proof report did not retain actual loopback request evidence."
}
foreach ($scenario in $report.scenarios) {
    if (-not $scenario.passed -or -not $scenario.baselineRestored) {
        throw "Experiment proof scenario failed or retained candidate allocation: $($scenario.proofId)"
    }
    if ($scenario.finalRecord.currentAllocation.kind -eq "CANDIDATE") {
        throw "Experiment proof scenario retained candidate routing: $($scenario.proofId)"
    }
}

Write-Host $text
Write-Host "PASS: Enterprise Lab $Suite experiment proof wrote immutable target-only evidence under $OutputDir"
