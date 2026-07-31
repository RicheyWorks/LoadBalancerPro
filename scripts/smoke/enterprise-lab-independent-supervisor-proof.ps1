param(
    [switch]$DryRun,
    [switch]$Package,
    [string]$OutputDir = "target/enterprise-lab-independent-supervisor-proof-smoke"
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "enterprise-lab-proof-tool-runtime.ps1")

function Resolve-RepoPath {
    param([string]$Path)
    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $Path))
}

function Assert-OutputUnderTarget {
    param([string]$Path)
    $targetRoot = Resolve-RepoPath "target"
    $resolvedOutput = Resolve-RepoPath $Path
    if (-not $resolvedOutput.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Enterprise Lab independent-supervisor proof output must stay under target/. Requested: $Path"
    }
    return $resolvedOutput
}

function Assert-SanitizedEvidence {
    param([string]$Text)
    foreach ($pattern in @(
        '(?i)bearer\s+[a-z0-9._~+/-]{12,}',
        '(?i)x-api-key\s*[:=]\s*[a-z0-9._~+/-]{8,}',
        '(?i)(password|secret|credential|token)\s*[:=]\s*[a-z0-9._~+/-]{8,}',
        'CHANGE_ME_LOCAL_API_KEY',
        '(?i)https?://(?!127\.0\.0\.1)')) {
        if ($Text -match $pattern) {
            throw "Refusing independent-supervisor proof evidence outside its sanitized loopback boundary."
        }
    }
}

$resolvedOutputDir = Assert-OutputUnderTarget -Path $OutputDir
if ($DryRun) {
    Write-Host "Enterprise Lab independent-supervisor proof dry run."
    Write-Host "Output directory: $OutputDir"
    Write-Host "Planned command: test-compile then EnterpriseLabProofToolsApplication --enterprise-lab-independent-supervisor-proof --enterprise-lab-independent-supervisor-proof-output=$OutputDir"
    Write-Host "Safety: bounded separate local JVM and literal-loopback proof only; no external target, cloud, tenant, multi-host, network-filesystem, or production action."
    exit 0
}

New-Item -ItemType Directory -Force -Path $resolvedOutputDir | Out-Null
$run = Invoke-EnterpriseLabProofTool -Arguments @(
    "--enterprise-lab-independent-supervisor-proof",
    "--enterprise-lab-independent-supervisor-proof-output=$OutputDir")
$text = ($run.Output -join "`n")
if ($run.ExitCode -ne 0) {
    throw "Enterprise Lab independent-supervisor proof tool failed with exit code $($run.ExitCode)`n$text"
}
Assert-SanitizedEvidence -Text $text

$reportPath = Join-Path $resolvedOutputDir "enterprise-lab-independent-supervisor-proof.json"
$summaryPath = Join-Path $resolvedOutputDir "enterprise-lab-independent-supervisor-proof-summary.md"
foreach ($path in @($reportPath, $summaryPath)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Expected independent-supervisor proof file missing: $path"
    }
    Assert-SanitizedEvidence -Text (Get-Content -LiteralPath $path -Raw)
}

$report = (Get-Content -LiteralPath $reportPath -Raw) | ConvertFrom-Json
foreach ($check in @(
        "independentInstalledStateSurvivedApplicationCrash",
        "staleApplicationRejected",
        "supervisorRestartReconciled",
        "applicationCrashAfterSupervisorApplyReconciled",
        "supervisorCrashWindowsReconciled",
        "competingSupervisorSingleWinner",
        "ipcBoundariesEnforced")) {
    if (-not $report.$check) {
        throw "Independent-supervisor proof check failed: $check"
    }
}
$crashWindows = @($report.supervisorCrashWindows.PSObject.Properties)
$ipcChecks = @($report.ipcBoundaryChecks.PSObject.Properties)
if ($crashWindows.Count -ne 8 -or @($crashWindows.Value | Where-Object { -not $_ }).Count -ne 0) {
    throw "Independent-supervisor proof did not pass all eight crash windows."
}
if ($ipcChecks.Count -ne 18 -or @($ipcChecks.Value | Where-Object { -not $_ }).Count -ne 0) {
    throw "Independent-supervisor proof did not pass all eighteen IPC checks."
}

Write-Host $text
Write-Host "PASS: independent application/supervisor crash-window and IPC proofs passed under $OutputDir"
