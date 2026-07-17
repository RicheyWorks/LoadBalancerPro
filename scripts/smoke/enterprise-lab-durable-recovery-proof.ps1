param(
    [switch]$DryRun,
    [switch]$Package,
    [string]$OutputDir = "target/enterprise-lab-durable-recovery-proof-smoke"
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
        throw "Enterprise Lab durable recovery proof output must stay under target/. Requested: $Path"
    }
    return $resolvedOutput
}

function Find-ExecutableJar {
    $jar = Get-ChildItem -Path "target" -Filter "LoadBalancerPro-*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "(-sources|-javadoc|-tests|\.original)\.jar$" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $jar) { return $null }
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
            throw "Refusing durable recovery proof output that looks secret-bearing."
        }
    }
}

$resolvedOutputDir = Assert-OutputUnderTarget -Path $OutputDir

if ($DryRun) {
    Write-Host "Enterprise Lab durable recovery proof dry run."
    Write-Host "Output directory: $OutputDir"
    Write-Host "Planned command: java -jar target/LoadBalancerPro-*.jar --enterprise-lab-durable-recovery-proof --enterprise-lab-durable-recovery-output=$OutputDir"
    Write-Host "Safety: foreground bounded literal-loopback and target-only local-filesystem proof; no API server, external target, operating-system crash claim, cloud, tenant, production routing, release, or registry action."
    exit 0
}

$jarPath = Find-ExecutableJar
if ($Package -or $null -eq $jarPath) {
    & mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "mvn package failed with exit code $LASTEXITCODE" }
    $jarPath = Find-ExecutableJar
}
if ($null -eq $jarPath) { throw "Executable LoadBalancerPro jar was not found under target/." }

New-Item -ItemType Directory -Force -Path $resolvedOutputDir | Out-Null
$output = & java -jar $jarPath "--enterprise-lab-durable-recovery-proof" "--enterprise-lab-durable-recovery-output=$OutputDir" 2>&1
$exitCode = $LASTEXITCODE
$text = ($output -join "`n")
if ($exitCode -ne 0) {
    throw "Enterprise Lab durable recovery proof CLI failed with exit code $exitCode`n$text"
}
Assert-NoSecretValues -Text $text

$reportPath = Join-Path $resolvedOutputDir "enterprise-lab-durable-recovery-proof.json"
$summaryPath = Join-Path $resolvedOutputDir "enterprise-lab-durable-recovery-proof-summary.md"
foreach ($path in @($reportPath, $summaryPath)) {
    if (-not (Test-Path -LiteralPath $path)) { throw "Expected durable proof file missing: $path" }
    Assert-NoSecretValues -Text (Get-Content -LiteralPath $path -Raw)
}

$reportText = Get-Content -LiteralPath $reportPath -Raw
if ($reportText -match '(?i)https?://(?!127\.0\.0\.1)') {
    throw "Durable recovery proof evidence contains a non-loopback target."
}
$report = $reportText | ConvertFrom-Json
if (-not $report.allPassed -or $report.actualLoopbackRequests -lt 1) {
    throw "Durable recovery proof report did not pass with actual loopback requests."
}
if ($report.interruptedFinalState -ne "ROLLED_BACK" -or
        $report.completedFinalState -ne "COMPLETED" -or
        $report.normalRollbackFinalState -ne "ROLLED_BACK") {
    throw "Durable recovery proof did not preserve expected terminal lifecycle states."
}
foreach ($check in @(
        "firstRecoveryAdmitted", "secondRecoveryIdempotent", "completedRestartPreserved",
        "normalRollbackRestartPreserved", "middleCorruptionQuarantined", "partialTailQuarantined",
        "unresolvedEvidenceRetained", "activeCompactionRejected", "terminalCompactionVerified")) {
    if (-not $report.$check) { throw "Durable recovery proof check failed: $check" }
}

Write-Host $text
Write-Host "PASS: durable restart, corruption quarantine, and terminal compaction proof passed under $OutputDir"
