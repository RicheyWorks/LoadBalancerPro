param(
    [switch]$DryRun,
    [switch]$Package,
    [string]$OutputDir = "target/enterprise-lab-ownership-proof-smoke"
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
        throw "Enterprise Lab ownership proof output must stay under target/. Requested: $Path"
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
    foreach ($pattern in @(
        '(?i)bearer\s+[a-z0-9._~+/-]{12,}',
        '(?i)x-api-key\s*[:=]\s*[a-z0-9._~+/-]{8,}',
        '(?i)(password|secret|credential|token)\s*[:=]\s*[a-z0-9._~+/-]{8,}',
        'CHANGE_ME_LOCAL_API_KEY')) {
        if ($Text -match $pattern) { throw "Refusing ownership proof output that looks secret-bearing." }
    }
}

$resolvedOutputDir = Assert-OutputUnderTarget -Path $OutputDir
if ($DryRun) {
    Write-Host "Enterprise Lab separate-process ownership proof dry run."
    Write-Host "Output directory: $OutputDir"
    Write-Host "Planned command: java -jar target/LoadBalancerPro-*.jar --enterprise-lab-ownership-proof --enterprise-lab-ownership-proof-output=$OutputDir"
    Write-Host "Safety: foreground bounded separate local JVM and literal-loopback proof; no API server, arbitrary path, external target, cloud, multi-host, network-filesystem, tenant, or production action."
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
$output = & java -jar $jarPath "--enterprise-lab-ownership-proof" "--enterprise-lab-ownership-proof-output=$OutputDir" 2>&1
$exitCode = $LASTEXITCODE
$text = ($output -join "`n")
if ($exitCode -ne 0) { throw "Enterprise Lab ownership proof CLI failed with exit code $exitCode`n$text" }
Assert-NoSecretValues -Text $text

$reportPath = Join-Path $resolvedOutputDir "enterprise-lab-ownership-proof.json"
$summaryPath = Join-Path $resolvedOutputDir "enterprise-lab-ownership-proof-summary.md"
foreach ($path in @($reportPath, $summaryPath)) {
    if (-not (Test-Path -LiteralPath $path)) { throw "Expected ownership proof file missing: $path" }
    Assert-NoSecretValues -Text (Get-Content -LiteralPath $path -Raw)
}

$reportText = Get-Content -LiteralPath $reportPath -Raw
if ($reportText -match '(?i)https?://(?!127\.0\.0\.1)') {
    throw "Ownership proof evidence contains a non-loopback target."
}
if ($reportText -match '(?i)(directoryIdentity|lockFileIdentity|processId|hostDiagnostic|fileHandle|rawLock)') {
    throw "Ownership proof evidence exposes a controlled-storage or process implementation detail."
}
$report = $reportText | ConvertFrom-Json
foreach ($check in @(
        "allPassed", "liveOwnerDenied", "ownerAppendAndReconciliationVerified",
        "nonOwnerAppendDenied", "nonOwnerCompactionDenied", "nonOwnerRetentionDenied",
        "nonOwnerExperimentStartDenied", "nonOwnerAllocationChangeDenied", "renewalSucceeded",
        "cleanReleaseRecorded", "repeatedReleaseIdempotent", "cleanTakeoverClassified",
        "restartedPriorOwnerDenied", "abruptStaleOwnerClassified", "journalsVerifiedAndReplayed",
        "interruptedExperimentRolledBack", "baselineRestorationVerified", "takeoverRecoveryRecorded",
        "repeatedRestartIdempotent", "simultaneousAcquisitionSingleWinner",
        "competingTakeoverSingleWinner")) {
    if (-not $report.$check) { throw "Ownership proof check failed: $check" }
}
if ($report.cleanTakeoverGeneration -le $report.initialGeneration -or
        $report.abruptTakeoverGeneration -le $report.cleanTakeoverGeneration) {
    throw "Ownership generations did not increase across clean and abrupt takeover."
}

Write-Host $text
Write-Host "PASS: separate-process exclusion, takeover race, and restart recovery passed under $OutputDir"
