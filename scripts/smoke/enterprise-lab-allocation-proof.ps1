param(
    [switch]$DryRun,
    [switch]$Package,
    [string]$OutputDir = "target/enterprise-lab-allocation-proof-smoke"
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
        throw "Enterprise Lab allocation proof output must stay under target/. Requested: $Path"
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

function Assert-SanitizedEvidence {
    param([string]$Text)
    foreach ($pattern in @(
        '(?i)bearer\s+[a-z0-9._~+/-]{12,}',
        '(?i)x-api-key\s*[:=]\s*[a-z0-9._~+/-]{8,}',
        '(?i)(password|secret|credential|token)\s*[:=]\s*[a-z0-9._~+/-]{8,}',
        'CHANGE_ME_LOCAL_API_KEY',
        '(?i)https?://(?!127\.0\.0\.1)',
        '(?i)(trustedRoot|dataDirectory|lockFileIdentity|stackTrace|authorizationHeader)')) {
        if ($Text -match $pattern) {
            throw "Refusing allocation proof evidence that violates its sanitized loopback boundary."
        }
    }
}

$resolvedOutputDir = Assert-OutputUnderTarget -Path $OutputDir
if ($DryRun) {
    Write-Host "Enterprise Lab allocation crash-window proof dry run."
    Write-Host "Output directory: $OutputDir"
    Write-Host "Planned command: java -jar target/LoadBalancerPro-*.jar --enterprise-lab-allocation-proof --enterprise-lab-allocation-proof-output=$OutputDir"
    Write-Host "Safety: bounded foreground proof and authenticated separate local holder JVM; no API server, arbitrary path, allocation override, external target, native command, cloud, tenant, or production action."
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
$output = & java -jar $jarPath "--enterprise-lab-allocation-proof" "--enterprise-lab-allocation-proof-output=$OutputDir" 2>&1
$exitCode = $LASTEXITCODE
$text = ($output -join "`n")
if ($exitCode -ne 0) { throw "Enterprise Lab allocation proof CLI failed with exit code $exitCode`n$text" }
Assert-SanitizedEvidence -Text $text

$reportPath = Join-Path $resolvedOutputDir "enterprise-lab-allocation-proof.json"
$summaryPath = Join-Path $resolvedOutputDir "enterprise-lab-allocation-proof-summary.md"
foreach ($path in @($reportPath, $summaryPath)) {
    if (-not (Test-Path -LiteralPath $path)) { throw "Expected allocation proof file missing: $path" }
    Assert-SanitizedEvidence -Text (Get-Content -LiteralPath $path -Raw)
}

$report = (Get-Content -LiteralPath $reportPath -Raw) | ConvertFrom-Json
foreach ($check in @(
        "normalTransactionPassed", "crashBeforeApplyPassed",
        "crashAfterApplyPassed", "crashAfterCommitPassed", "staleOwnerTakeoverPassed",
        "competingStaleMutationDenied", "restorationFailureClosedAdmission",
        "repeatedReconciliationStable", "externalHolderSeparateProcess")) {
    if (-not $report.$check) { throw "Allocation proof check failed: $check" }
}
$driftProperties = @($report.driftClassifications.PSObject.Properties)
if ($driftProperties.Count -ne 10) {
    throw "Allocation proof did not emit exactly ten controlled drift classifications."
}
foreach ($classification in $driftProperties.Value) {
    if ([string]::IsNullOrWhiteSpace($classification) -or $classification -eq "SAFE_BASELINE_INSTALLED") {
        throw "Allocation proof silently accepted a controlled drift case."
    }
}

Write-Host $text
Write-Host "PASS: allocation transaction, crash-window, takeover, drift, restoration-failure, and repeated-reconciliation proofs passed under $OutputDir"
