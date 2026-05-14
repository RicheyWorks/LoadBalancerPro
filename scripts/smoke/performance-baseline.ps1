param(
    [switch]$DryRun,
    [switch]$Package,
    [int]$Port = 19680,
    [string]$FixturePath = "docs/performance/performance-fixtures.json",
    [string]$ThresholdPath = "docs/performance/performance-thresholds.example.json",
    [string]$OutputDir = "target/performance-baseline"
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
        throw "Performance baseline output must stay under target/. Requested: $Path"
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
            throw "Refusing to write performance baseline evidence that looks like it contains a secret."
        }
    }
}

function Write-EvidenceFile {
    param(
        [string]$Path,
        [string]$Content
    )

    Assert-NoSecretValues -Text $Content
    Set-Content -LiteralPath $Path -Value $Content -Encoding UTF8
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

function Invoke-LocalRequest {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Body,
        [int]$TimeoutSeconds
    )

    $request = @{
        Uri = "http://127.0.0.1:$Port$Path"
        Method = $Method
        TimeoutSec = $TimeoutSeconds
        UseBasicParsing = $true
        ErrorAction = "Stop"
    }
    if ($Body) {
        $request["ContentType"] = "application/json"
        $request["Body"] = $Body
    }

    try {
        $response = Invoke-WebRequest @request
        return [pscustomobject]@{
            statusCode = [int]$response.StatusCode
            success = $true
            error = $null
        }
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [pscustomobject]@{
                statusCode = [int]$_.Exception.Response.StatusCode
                success = $false
                error = "http_status"
            }
        }
        return [pscustomobject]@{
            statusCode = 0
            success = $false
            error = $_.Exception.GetType().Name
        }
    }
}

function Wait-ForApp {
    for ($attempt = 1; $attempt -le 90; $attempt++) {
        $response = Invoke-LocalRequest -Method "GET" -Path "/api/health" -TimeoutSeconds 2
        if ($response.statusCode -eq 200) {
            return
        }
        Start-Sleep -Milliseconds 500
    }
    throw "Timed out waiting for local app on 127.0.0.1:$Port"
}

function Percentile {
    param(
        [double[]]$Values,
        [double]$Percent
    )

    if ($Values.Count -eq 0) {
        return 0.0
    }
    $sorted = $Values | Sort-Object
    $index = [Math]::Ceiling(($Percent / 100.0) * $sorted.Count) - 1
    $index = [Math]::Max(0, [Math]::Min($index, $sorted.Count - 1))
    return [Math]::Round([double]$sorted[$index], 3)
}

function Average {
    param([double[]]$Values)
    if ($Values.Count -eq 0) {
        return 0.0
    }
    return [Math]::Round((($Values | Measure-Object -Average).Average), 3)
}

function ThresholdForFixture {
    param(
        [object]$Thresholds,
        [string]$FixtureId
    )

    $default = $Thresholds.defaults
    $specific = $Thresholds.fixtures | Where-Object { $_.id -eq $FixtureId } | Select-Object -First 1
    return [pscustomobject]@{
        p95WarningMillis = if ($specific -and $specific.p95WarningMillis) { [double]$specific.p95WarningMillis } else { [double]$default.p95WarningMillis }
        p99WarningMillis = if ($specific -and $specific.p99WarningMillis) { [double]$specific.p99WarningMillis } else { [double]$default.p99WarningMillis }
        errorRateWarningPercent = if ($specific -and $null -ne $specific.errorRateWarningPercent) { [double]$specific.errorRateWarningPercent } else { [double]$default.errorRateWarningPercent }
    }
}

function Measure-Fixture {
    param(
        [object]$Fixture,
        [int]$WarmupRequests,
        [int]$MeasuredRequests,
        [int]$TimeoutSeconds,
        [object]$Thresholds
    )

    $body = $null
    if ($Fixture.PSObject.Properties.Name -contains "body") {
        $body = $Fixture.body | ConvertTo-Json -Depth 32 -Compress
    }

    for ($i = 0; $i -lt $WarmupRequests; $i++) {
        [void](Invoke-LocalRequest -Method $Fixture.method -Path $Fixture.path -Body $body -TimeoutSeconds $TimeoutSeconds)
    }

    $latencies = New-Object System.Collections.Generic.List[double]
    $successCount = 0
    $errorCount = 0
    $statusCounts = @{}
    for ($i = 0; $i -lt $MeasuredRequests; $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $response = Invoke-LocalRequest -Method $Fixture.method -Path $Fixture.path -Body $body -TimeoutSeconds $TimeoutSeconds
        $sw.Stop()
        $latencies.Add([Math]::Round($sw.Elapsed.TotalMilliseconds, 3))
        $key = [string]$response.statusCode
        if (-not $statusCounts.ContainsKey($key)) {
            $statusCounts[$key] = 0
        }
        $statusCounts[$key]++
        if ($response.statusCode -eq [int]$Fixture.expectedStatus) {
            $successCount++
        } else {
            $errorCount++
        }
    }

    $values = [double[]]$latencies.ToArray()
    $threshold = ThresholdForFixture -Thresholds $Thresholds -FixtureId $Fixture.id
    $errorRate = if ($MeasuredRequests -eq 0) { 0.0 } else { [Math]::Round(($errorCount / [double]$MeasuredRequests) * 100.0, 3) }
    $summary = [pscustomobject]@{
        id = $Fixture.id
        displayName = $Fixture.displayName
        category = $Fixture.category
        method = $Fixture.method
        path = $Fixture.path
        expectedStatus = [int]$Fixture.expectedStatus
        requestCount = $MeasuredRequests
        successCount = $successCount
        errorCount = $errorCount
        errorRatePercent = $errorRate
        minLatencyMillis = [Math]::Round(($values | Measure-Object -Minimum).Minimum, 3)
        maxLatencyMillis = [Math]::Round(($values | Measure-Object -Maximum).Maximum, 3)
        averageLatencyMillis = Average -Values $values
        p50LatencyMillis = Percentile -Values $values -Percent 50
        p95LatencyMillis = Percentile -Values $values -Percent 95
        p99LatencyMillis = Percentile -Values $values -Percent 99
        statusCounts = $statusCounts
        threshold = $threshold
        thresholdStatus = "pass"
        thresholdWarnings = @()
    }

    $warnings = New-Object System.Collections.Generic.List[string]
    if ($summary.p95LatencyMillis -gt $threshold.p95WarningMillis) {
        $warnings.Add("p95 latency $($summary.p95LatencyMillis)ms exceeded warning threshold $($threshold.p95WarningMillis)ms")
    }
    if ($summary.p99LatencyMillis -gt $threshold.p99WarningMillis) {
        $warnings.Add("p99 latency $($summary.p99LatencyMillis)ms exceeded warning threshold $($threshold.p99WarningMillis)ms")
    }
    if ($summary.errorRatePercent -gt $threshold.errorRateWarningPercent) {
        $warnings.Add("error rate $($summary.errorRatePercent)% exceeded warning threshold $($threshold.errorRateWarningPercent)%")
    }
    if ($warnings.Count -gt 0) {
        $summary.thresholdStatus = "warning"
        $summary.thresholdWarnings = [string[]]$warnings.ToArray()
    }
    return $summary
}

function Git-Value {
    param([string[]]$GitArgs)
    try {
        $value = & git @GitArgs 2>$null
        if ($LASTEXITCODE -eq 0) {
            return ($value -join " ").Trim()
        }
    } catch {
    }
    return "unavailable"
}

function Project-Version {
    try {
        [xml]$pom = Get-Content -LiteralPath "pom.xml"
        return $pom.project.version
    } catch {
        return "unavailable"
    }
}

function Java-Version {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $output = & java -version 2>&1
        return (($output | ForEach-Object { $_.ToString() }) -join " ").Trim()
    } catch {
        return "unavailable"
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

$resolvedOutputDir = Assert-OutputUnderTarget -Path $OutputDir
if (-not (Test-Path -LiteralPath $FixturePath)) {
    throw "Performance fixture catalog missing: $FixturePath"
}
if (-not (Test-Path -LiteralPath $ThresholdPath)) {
    throw "Performance threshold config missing: $ThresholdPath"
}

if ($DryRun) {
    Write-Host "Performance baseline dry run."
    Write-Host "Fixture catalog: $FixturePath"
    Write-Host "Thresholds: $ThresholdPath"
    Write-Host "Output directory: $OutputDir"
    Write-Host "Planned local loopback app: http://127.0.0.1:$Port"
    Write-Host "Safety: local/package mode only; no live cloud, private network, external IdP, release, tag, asset, container, or registry action."
    exit 0
}

try {
    $fixtureCatalog = Get-Content -LiteralPath $FixturePath -Raw | ConvertFrom-Json
    $thresholds = Get-Content -LiteralPath $ThresholdPath -Raw | ConvertFrom-Json
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

    $job = Start-Job -Name "loadbalancerpro-performance-baseline-$Port" -ArgumentList $jarPath, $Port -ScriptBlock {
        param($JarPath, $AppPort)
        & java -jar $JarPath "--server.address=127.0.0.1" "--server.port=$AppPort" "--spring.profiles.active=local"
    }
    $StartedJobs += $job
    Wait-ForApp

    $results = @()
    foreach ($fixture in $fixtureCatalog.fixtures) {
        $results += Measure-Fixture -Fixture $fixture `
            -WarmupRequests ([int]$fixtureCatalog.warmupRequests) `
            -MeasuredRequests ([int]$fixtureCatalog.measuredRequests) `
            -TimeoutSeconds ([int]$fixtureCatalog.timeoutSeconds) `
            -Thresholds $thresholds
    }

    $totalRequests = ($results | Measure-Object -Property requestCount -Sum).Sum
    $totalErrors = ($results | Measure-Object -Property errorCount -Sum).Sum
    $warningCount = ($results | Where-Object { $_.thresholdStatus -eq "warning" }).Count
    $overallStatus = if ($totalErrors -gt 0) { "failed" } elseif ($warningCount -gt 0) { "warning" } else { "pass" }
    $metadata = [ordered]@{
        generatedBy = "performance-baseline.ps1"
        generatedAtUtc = [DateTime]::UtcNow.ToString("o")
        commit = Git-Value -GitArgs @("rev-parse", "HEAD")
        branch = Git-Value -GitArgs @("branch", "--show-current")
        projectVersion = Project-Version
        javaVersion = Java-Version
        os = [System.Runtime.InteropServices.RuntimeInformation]::OSDescription
        port = $Port
        fixtureVersion = $fixtureCatalog.fixtureVersion
        thresholdVersion = $thresholds.thresholdVersion
        scope = "local loopback lab-grade baseline; not production performance certification"
    }

    $report = [ordered]@{
        metadata = $metadata
        summary = [ordered]@{
            fixtureCount = $results.Count
            totalRequests = $totalRequests
            totalErrors = $totalErrors
            totalErrorRatePercent = if ($totalRequests -eq 0) { 0.0 } else { [Math]::Round(($totalErrors / [double]$totalRequests) * 100.0, 3) }
            thresholdWarnings = $warningCount
            overallStatus = $overallStatus
            warning = "Results are local/lab-grade only and are not production SLO, SLA, or capacity evidence."
        }
        fixtures = $results
    }

    $thresholdReport = [ordered]@{
        mode = $thresholds.mode
        overallStatus = $overallStatus
        warnings = @($results | Where-Object { $_.thresholdStatus -eq "warning" } | ForEach-Object {
            [ordered]@{
                id = $_.id
                warnings = $_.thresholdWarnings
            }
        })
    }

    $dashboard = [ordered]@{
        generatedAtUtc = $metadata.generatedAtUtc
        commit = $metadata.commit
        projectVersion = $metadata.projectVersion
        dashboardFields = @(
            "fixtureId",
            "category",
            "requestCount",
            "successCount",
            "errorCount",
            "errorRatePercent",
            "averageLatencyMillis",
            "p50LatencyMillis",
            "p95LatencyMillis",
            "p99LatencyMillis",
            "thresholdStatus"
        )
        fixtures = @($results | ForEach-Object {
            [ordered]@{
                fixtureId = $_.id
                category = $_.category
                requestCount = $_.requestCount
                successCount = $_.successCount
                errorCount = $_.errorCount
                errorRatePercent = $_.errorRatePercent
                averageLatencyMillis = $_.averageLatencyMillis
                p50LatencyMillis = $_.p50LatencyMillis
                p95LatencyMillis = $_.p95LatencyMillis
                p99LatencyMillis = $_.p99LatencyMillis
                thresholdStatus = $_.thresholdStatus
            }
        })
        warning = "Dashboard-ready local evidence only; not production SLO certification."
    }

    $reportJson = $report | ConvertTo-Json -Depth 20
    $thresholdJson = $thresholdReport | ConvertTo-Json -Depth 12
    $dashboardJson = $dashboard | ConvertTo-Json -Depth 12
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "performance-report.json") -Content $reportJson
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "performance-threshold-results.json") -Content $thresholdJson
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "performance-dashboard.json") -Content $dashboardJson

    $lines = @(
        "# LoadBalancerPro Performance Baseline Evidence",
        "",
        "Status: $overallStatus",
        "Commit: $($metadata.commit)",
        "Project version: $($metadata.projectVersion)",
        "Scope: local loopback lab-grade baseline; not production SLO, SLA, or capacity evidence.",
        "",
        "| Fixture | Requests | Errors | Avg ms | p50 ms | p95 ms | p99 ms | Threshold |",
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |"
    )
    foreach ($result in $results) {
        $lines += "| $($result.id) | $($result.requestCount) | $($result.errorCount) | $($result.averageLatencyMillis) | $($result.p50LatencyMillis) | $($result.p95LatencyMillis) | $($result.p99LatencyMillis) | $($result.thresholdStatus) |"
    }
    $lines += ""
    $lines += "Evidence files: performance-report.json, performance-dashboard.json, performance-threshold-results.json."
    $lines += "Safety: no live cloud, private network, external IdP, release, tag, asset, container, registry, or release artifact action."
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "performance-summary.md") -Content ($lines -join "`n")

    $manifest = [ordered]@{
        generatedBy = "performance-baseline.ps1"
        outputDirectory = $resolvedOutputDir
        fixtureCatalog = $FixturePath
        thresholds = $ThresholdPath
        files = @(
            "performance-report.json",
            "performance-dashboard.json",
            "performance-threshold-results.json",
            "performance-summary.md"
        )
        safety = "Local loopback lab-grade evidence only; no secrets, live cloud, private network, release action, or container publication."
    } | ConvertTo-Json -Depth 6
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "performance-evidence-manifest.json") -Content $manifest

    if ($totalErrors -gt 0) {
        throw "Performance baseline completed with $totalErrors request errors. Evidence was written under $OutputDir."
    }
    Write-Host "PASS: Performance baseline evidence written under $OutputDir with status $overallStatus"
} finally {
    foreach ($job in $StartedJobs) {
        Stop-Job -Job $job -ErrorAction SilentlyContinue
        Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
    }
}
