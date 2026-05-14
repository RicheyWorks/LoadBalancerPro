param(
    [switch]$DryRun,
    [switch]$Package,
    [int]$Port = 19480,
    [string]$OutputDir = "target/enterprise-lab-observability"
)

$ErrorActionPreference = "Stop"
$StartedJobs = @()

function Resolve-RepoPath {
    param([string]$Path)
    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $Path))
}

function Assert-OutputUnderTarget {
    param([string]$Path)

    $targetRoot = Resolve-RepoPath "target"
    $resolvedOutput = Resolve-RepoPath $Path
    if (-not $resolvedOutput.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Enterprise Lab observability output must stay under target/. Requested: $Path"
    }
    return $resolvedOutput
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
            throw "Refusing to write Enterprise Lab observability evidence that looks like it contains a secret."
        }
    }
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

function Invoke-LabRequest {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Body
    )

    $request = @{
        Uri = "http://127.0.0.1:$Port$Path"
        Method = $Method
        TimeoutSec = 5
        UseBasicParsing = $true
        ErrorAction = "Stop"
    }
    if ($Body) {
        $request["ContentType"] = "application/json"
        $request["Body"] = $Body
    }
    return Invoke-WebRequest @request
}

function Wait-ForApp {
    for ($attempt = 1; $attempt -le 90; $attempt++) {
        try {
            $response = Invoke-LabRequest -Method "GET" -Path "/api/health"
            if ([int]$response.StatusCode -eq 200) {
                return
            }
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
    throw "Timed out waiting for local app on 127.0.0.1:$Port"
}

function Write-EvidenceFile {
    param(
        [string]$Path,
        [string]$Content
    )

    Assert-NoSecretValues -Text $Content
    Set-Content -LiteralPath $Path -Value $Content -Encoding UTF8
}

$resolvedOutputDir = Assert-OutputUnderTarget -Path $OutputDir

if ($DryRun) {
    Write-Host "Enterprise Lab observability pack dry run."
    Write-Host "Output directory: $OutputDir"
    Write-Host "Planned local loopback app: http://127.0.0.1:$Port"
    Write-Host "Planned checks: scenarios, lab runs, policy, audit-events, metrics JSON, Prometheus-style metrics."
    Write-Host "Safety: local/package mode only; no live cloud, private network, release, tag, asset, container, registry, or release artifact download action."
    exit 0
}

try {
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

    $job = Start-Job -Name "loadbalancerpro-enterprise-lab-observability-$Port" -ArgumentList $jarPath, $Port -ScriptBlock {
        param($JarPath, $AppPort)
        & java -jar $JarPath "--server.address=127.0.0.1" "--server.port=$AppPort" "--spring.profiles.active=local"
    }
    $StartedJobs += $job
    Wait-ForApp

    $scenarioCatalog = (Invoke-LabRequest -Method "GET" -Path "/api/lab/scenarios").Content
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "scenario-catalog.json") -Content $scenarioCatalog

    $runBodies = @(
        @{ mode = "shadow"; body = '{"mode":"shadow","scenarioIds":["normal-balanced-load","tail-latency-pressure"]}' },
        @{ mode = "recommend"; body = '{"mode":"recommend","scenarioIds":["tail-latency-pressure","stale-signal"]}' },
        @{ mode = "active-experiment"; body = '{"mode":"active-experiment","scenarioIds":["normal-balanced-load","conflicting-signal","all-unhealthy-degradation"]}' }
    )
    foreach ($entry in $runBodies) {
        $run = (Invoke-LabRequest -Method "POST" -Path "/api/lab/runs" -Body $entry.body).Content
        Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "lab-run-$($entry.mode).json") -Content $run
    }

    $policy = (Invoke-LabRequest -Method "GET" -Path "/api/lab/policy").Content
    $audit = (Invoke-LabRequest -Method "GET" -Path "/api/lab/audit-events").Content
    $metrics = (Invoke-LabRequest -Method "GET" -Path "/api/lab/metrics").Content
    $prometheus = (Invoke-LabRequest -Method "GET" -Path "/api/lab/metrics/prometheus").Content

    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "policy-status.json") -Content $policy
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "policy-audit-events.json") -Content $audit
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "lab-metrics.json") -Content $metrics
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "lab-metrics.prom") -Content $prometheus

    $metricsJson = $metrics | ConvertFrom-Json
    $auditJson = $audit | ConvertFrom-Json
    $summary = @(
        "# Enterprise Lab Observability Pack Evidence",
        "",
        "Output directory: $resolvedOutputDir",
        "",
        "| Signal | Value |",
        "| --- | --- |",
        "| Lab runs created | $($metricsJson.labRunsCreated) |",
        "| Scenarios executed | $($metricsJson.labScenariosExecuted) |",
        "| Recommendations produced | $($metricsJson.recommendationsProduced) |",
        "| Active-experiment changes allowed | $($metricsJson.activeExperimentChangesAllowed) |",
        "| Rollback/fail-closed events | $($metricsJson.rollbackFailClosedEvents) |",
        "| Audit events returned | $($auditJson.count) |",
        "",
        "References:",
        "- docs/observability/grafana-enterprise-lab-dashboard.json",
        "- docs/observability/enterprise-lab-alerts.yml",
        "- docs/observability/SLO_TEMPLATES.md",
        "",
        "Safety: process-local lab-grade evidence only; no live cloud, private network, release, tag, asset, container, registry, or release artifact download action."
    ) -join "`n"
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "observability-summary.md") -Content $summary

    $manifest = [ordered]@{
        generatedBy = "enterprise-lab-observability-pack.ps1"
        outputDirectory = $resolvedOutputDir
        mode = "local/package loopback"
        files = @(
            "scenario-catalog.json",
            "lab-run-shadow.json",
            "lab-run-recommend.json",
            "lab-run-active-experiment.json",
            "policy-status.json",
            "policy-audit-events.json",
            "lab-metrics.json",
            "lab-metrics.prom",
            "observability-summary.md"
        )
        safety = "No secrets, live cloud, private network, release, tag, asset, container, registry, or release artifact download action."
    } | ConvertTo-Json -Depth 4
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "observability-evidence-manifest.json") -Content $manifest

    Write-Host "PASS: Enterprise Lab observability evidence written under $OutputDir"
} finally {
    foreach ($job in $StartedJobs) {
        Stop-Job -Job $job -ErrorAction SilentlyContinue
        Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
    }
}
