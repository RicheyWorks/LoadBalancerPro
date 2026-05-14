param(
    [switch]$DryRun,
    [switch]$Package,
    [string]$OutputDir = "target/controlled-adaptive-routing"
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
        throw "Controlled adaptive-routing policy output must stay under target/. Requested: $Path"
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
            throw "Refusing to write controlled adaptive-routing policy output that looks secret-bearing."
        }
    }
}

$resolvedOutputDir = Assert-OutputUnderTarget -Path $OutputDir
$summaryPath = Join-Path $resolvedOutputDir "controlled-adaptive-routing-policy-summary.md"
$metadataPath = Join-Path $resolvedOutputDir "controlled-adaptive-routing-policy-metadata.json"
$modes = @("off", "shadow", "recommend", "active-experiment")

if ($DryRun) {
    Write-Host "Controlled adaptive-routing policy dry run."
    Write-Host "Output directory: $OutputDir"
    foreach ($mode in $modes) {
        Write-Host "Planned command: java -jar target/LoadBalancerPro-*.jar --enterprise-lab-workflow=$mode --enterprise-lab-output=$OutputDir/$mode"
    }
    Write-Host "Safety: local deterministic CLI only; no API server, cloud mutation, external network, release publication, container publication, or registry action."
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
$modeSummaries = @()

foreach ($mode in $modes) {
    $modeOutput = Join-Path $resolvedOutputDir $mode
    New-Item -ItemType Directory -Force -Path $modeOutput | Out-Null
    $output = & java -jar $jarPath "--enterprise-lab-workflow=$mode" "--enterprise-lab-output=$modeOutput" 2>&1
    $exitCode = $LASTEXITCODE
    $combined = ($output -join "`n")
    Assert-NoSecretValues -Text $combined
    if ($exitCode -ne 0) {
        throw "Enterprise Lab workflow failed for mode $mode with exit code $exitCode"
    }

    $runPath = Join-Path $modeOutput "enterprise-lab-run.json"
    $run = Get-Content -LiteralPath $runPath -Raw | ConvertFrom-Json
    $changedCount = @($run.results | Where-Object { $_.resultChanged -eq $true }).Count
    $auditCount = @($run.policyAuditEvents).Count
    $guardrails = @($run.policyAuditEvents | ForEach-Object { $_.guardrailReasons } | Select-Object -Unique)
    $modeSummaries += [ordered]@{
        mode = $mode
        runId = $run.runId
        scenarioCount = $run.scorecard.totalScenarios
        changedCount = $changedCount
        auditEventCount = $auditCount
        guardrails = $guardrails
        outputDirectory = $modeOutput
    }
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Controlled Active LASE Policy Evidence")
$lines.Add("")
$lines.Add("- Generated at: ``$((Get-Date).ToUniversalTime().ToString("o"))``")
$lines.Add("- Git commit: ``$(& git rev-parse HEAD)``")
$lines.Add("- Evidence path: ``$resolvedOutputDir``")
$lines.Add("- Safety: deterministic local Enterprise Lab workflow only; no cloud mutation, external network, release publication, container publication, or registry action.")
$lines.Add("")
$lines.Add("| Mode | Run | Scenarios | Changed decisions | Audit events | Guardrails |")
$lines.Add("| --- | --- | --- | --- | --- | --- |")
foreach ($summary in $modeSummaries) {
    $lines.Add("| $($summary.mode) | $($summary.runId) | $($summary.scenarioCount) | $($summary.changedCount) | $($summary.auditEventCount) | $(($summary.guardrails -join '; ').Replace('|', '\\|')) |")
}
$lines.Add("")
$lines.Add("## Rollback posture")
$lines.Add("")
$lines.Add("`off`, `shadow`, and `recommend` keep baseline final. `active-experiment` changes lab output only when guarded policy checks pass; blocked cases keep baseline with rollback reasons in each mode directory.")

$summaryText = $lines -join "`n"
Assert-NoSecretValues -Text $summaryText
Set-Content -LiteralPath $summaryPath -Value $summaryText -Encoding UTF8

$metadata = [ordered]@{
    generatedAt = (Get-Date).ToUniversalTime().ToString("o")
    gitCommit = (& git rev-parse HEAD)
    jarPath = $jarPath
    outputDirectory = $resolvedOutputDir
    summaryPath = $summaryPath
    modes = $modeSummaries
    safety = "target-only deterministic evidence; no cloud mutation, external network, release publication, container publication, or registry action"
}
Set-Content -LiteralPath $metadataPath -Value ($metadata | ConvertTo-Json -Depth 8) -Encoding UTF8

Write-Host "PASS: controlled adaptive-routing policy evidence written to $resolvedOutputDir"
Write-Host "PASS: summary written to $summaryPath"
