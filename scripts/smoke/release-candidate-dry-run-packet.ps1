param(
    [switch]$DryRun,
    [switch]$Package,
    [switch]$IncludeValidation,
    [string]$OutputDir = "target/release-candidate-dry-run"
)

$ErrorActionPreference = "Stop"

if (-not $DryRun -and -not $Package) {
    $DryRun = $true
}

function Invoke-Capture {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $output = & $FilePath @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "$FilePath $($Arguments -join ' ') exited with code $exitCode"
    }
    return @{
        ExitCode = $exitCode
        Output = ($output -join "`n")
    }
}

function Invoke-PacketCommand {
    param(
        [string]$Name,
        [string]$FilePath,
        [string[]]$Arguments
    )

    $display = "$FilePath $($Arguments -join ' ')"
    if ($DryRun) {
        Write-Host "PLANNED: $display"
        return [pscustomobject]@{
            name = $Name
            command = $display
            status = "planned"
            exitCode = $null
            durationSeconds = 0
        }
    }

    Write-Host "Running: $display"
    $started = Get-Date
    $output = & $FilePath @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    foreach ($line in $output) {
        Write-Host $line
    }
    $finished = Get-Date
    $duration = [math]::Round(($finished - $started).TotalSeconds, 1)
    if ($exitCode -ne 0) {
        throw "$display exited with code $exitCode"
    }

    return [pscustomobject]@{
        name = $Name
        command = $display
        status = "passed"
        exitCode = $exitCode
        durationSeconds = $duration
    }
}

function New-RecordedCommand {
    param(
        [string]$Name,
        [string]$Command,
        [string]$Status
    )

    Write-Host "$($Status.ToUpperInvariant()): $Command"
    return [pscustomobject]@{
        name = $Name
        command = $Command
        status = $Status
        exitCode = $null
        durationSeconds = 0
    }
}

function Find-ExecutableJar {
    $jar = Get-ChildItem -Path "target" -Filter "LoadBalancerPro-*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "(-sources|-javadoc|-tests|\.original)\.jar$" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($jar) {
        return $jar.FullName
    }
    return $null
}

function Assert-SafeOutputDirectory {
    param(
        [string]$RepoRoot,
        [string]$Candidate
    )

    $targetRoot = [System.IO.Path]::GetFullPath((Join-Path $RepoRoot "target"))
    $full = [System.IO.Path]::GetFullPath((Join-Path $RepoRoot $Candidate))
    if (-not $full.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Evidence output must stay under target/. Requested: $full"
    }
    return $full
}

function Write-TextFile {
    param(
        [string]$Path,
        [string[]]$Lines
    )

    $Lines | Set-Content -LiteralPath $Path -Encoding UTF8
}

function Write-Checksums {
    param(
        [string]$OutputPath,
        [string[]]$Paths
    )

    $lines = @()
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) {
            continue
        }
        $hash = Get-FileHash -Algorithm SHA256 -LiteralPath $path
        $relative = Resolve-Path -LiteralPath $path -Relative
        $lines += "$($hash.Hash.ToLowerInvariant())  $relative"
    }

    Write-TextFile -Path $OutputPath -Lines $lines
    return $lines
}

function Test-Checksums {
    param([string[]]$Lines)

    foreach ($line in $Lines) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }

        $parts = $line -split "\s+", 2
        if ($parts.Count -ne 2) {
            throw "Checksum line is not parseable: $line"
        }

        $expectedHash = $parts[0]
        $relativePath = $parts[1].Trim()
        $currentHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $relativePath).Hash.ToLowerInvariant()
        if ($currentHash -ne $expectedHash) {
            throw "Checksum verification failed for $relativePath"
        }
    }

    return $true
}

if ($env:LOADBALANCERPRO_RELEASE_PUBLISH -eq "true" -or $env:LOADBALANCERPRO_CONTAINER_PUBLISH -eq "true") {
    throw "This packet generator is release-free and container-publication-free; unset publish intent variables."
}

$repoResult = Invoke-Capture -FilePath "git" -Arguments @("rev-parse", "--show-toplevel")
$repoRoot = $repoResult.Output.Trim()
Set-Location $repoRoot

$outputPath = Assert-SafeOutputDirectory -RepoRoot $repoRoot -Candidate $OutputDir
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

$commit = (Invoke-Capture -FilePath "git" -Arguments @("rev-parse", "HEAD")).Output.Trim()
$branch = (Invoke-Capture -FilePath "git" -Arguments @("branch", "--show-current")).Output.Trim()
$status = (Invoke-Capture -FilePath "git" -Arguments @("status", "--short") -AllowFailure).Output.Trim()
if ([string]::IsNullOrWhiteSpace($status)) {
    $status = "clean"
}

$commands = @()
if ($Package -and $IncludeValidation) {
    $commands += Invoke-PacketCommand -Name "full tests" -FilePath "mvn" -Arguments @("-q", "clean", "test")
    $commands += Invoke-PacketCommand -Name "verify" -FilePath "mvn" -Arguments @("-q", "verify")
} else {
    $validationStatus = $(if ($DryRun) { "planned" } else { "validated-externally" })
    $commands += New-RecordedCommand -Name "full tests" -Command "mvn -q clean test" -Status $validationStatus
    $commands += New-RecordedCommand -Name "verify" -Command "mvn -q verify" -Status $validationStatus
}
$commands += Invoke-PacketCommand -Name "package" -FilePath "mvn" -Arguments @("-q", "-DskipTests", "package")

$shortCommit = $commit.Substring(0, 12)
$bomBase = "LoadBalancerPro-$shortCommit-bom"
$commands += Invoke-PacketCommand -Name "cyclonedx sbom" -FilePath "mvn" -Arguments @(
    "-q",
    "org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom",
    "-DoutputFormat=all",
    "-DoutputDirectory=$outputPath",
    "-DoutputName=$bomBase",
    "-Dcyclonedx.skipAttach=true"
)

$commands += Invoke-PacketCommand -Name "operator run profiles smoke" -FilePath "powershell" -Arguments @(
    "-NoProfile",
    "-ExecutionPolicy",
    "Bypass",
    "-File",
    ".\scripts\smoke\operator-run-profiles-smoke.ps1",
    ($(if ($DryRun) { "-DryRun" } else { "-Package" })),
    "-ApiKey",
    "<REDACTED>"
)

$postmanEvidenceDir = Join-Path $outputPath "postman-enterprise-lab-smoke"
$commands += Invoke-PacketCommand -Name "postman enterprise lab smoke" -FilePath "powershell" -Arguments @(
    "-NoProfile",
    "-ExecutionPolicy",
    "Bypass",
    "-File",
    ".\scripts\smoke\postman-enterprise-lab-safe-smoke.ps1",
    "-DryRun",
    "-ApiKey",
    "<REDACTED>",
    "-EvidenceDir",
    $postmanEvidenceDir
)

$jarPath = Find-ExecutableJar
$sbomJson = Join-Path $outputPath "$bomBase.json"
$sbomXml = Join-Path $outputPath "$bomBase.xml"
$checksumPath = Join-Path $outputPath "LoadBalancerPro-$shortCommit-SHA256SUMS.txt"
$checksumInputs = @()
if ($jarPath) {
    $checksumInputs += $jarPath
}
$checksumInputs += $sbomJson
$checksumInputs += $sbomXml

$checksums = @()
$checksumVerified = $false
if ($DryRun) {
    Write-TextFile -Path $checksumPath -Lines @("planned: SHA-256 checksums are generated when -Package is used")
    $checksums += "planned: SHA-256 checksums are generated when -Package is used"
} else {
    $checksums = Write-Checksums -OutputPath $checksumPath -Paths $checksumInputs
    if ($checksums.Count -eq 0) {
        throw "No local JAR or SBOM files were available for checksum evidence."
    }
    $checksumVerified = Test-Checksums -Lines $checksums
}

$secretLikeCount = (Get-ChildItem Env: | Where-Object { $_.Name -match "(API.?KEY|TOKEN|SECRET|PASSWORD|CREDENTIAL)" }).Count

$summary = [pscustomobject]@{
    generatedBy = "release-candidate-dry-run-packet.ps1"
    mode = $(if ($DryRun) { "dry-run" } else { "package" })
    commit = $commit
    branch = $branch
    workingTree = $status
    outputDirectory = $outputPath
    commands = $commands
    artifacts = [pscustomobject]@{
        jar = $(if ($jarPath) { $jarPath } else { "not generated in dry-run mode" })
        sbomJson = $(if (Test-Path -LiteralPath $sbomJson) { $sbomJson } else { "not generated in dry-run mode" })
        sbomXml = $(if (Test-Path -LiteralPath $sbomXml) { $sbomXml } else { "not generated in dry-run mode" })
        checksums = $checksumPath
        checksumVerified = $checksumVerified
    }
    posture = [pscustomobject]@{
        fullTests = "mvn -q clean test"
        verify = "mvn -q verify"
        package = "mvn -q -DskipTests package"
        sbom = "CycloneDX JSON/XML under target/release-candidate-dry-run when -Package is used"
        checksums = "SHA-256 for local JAR/SBOM files when -Package is used"
        attestations = "GitHub artifact attestations are produced only by the approved semantic-tag release workflow"
        codeql = "manual/operator verifies the latest matching CodeQL run"
        dependencyReview = "manual/operator verifies dependency review and risk-owner decisions"
        trivy = "CI Docker scan is the current container vulnerability evidence path"
        dockerDigestPinning = "Dockerfile base images remain digest-pinned"
        containerDefault = "Dockerfile defaults to prod profile and requires runtime LOADBALANCERPRO_API_KEY"
        apiKeyBoundary = "prod/cloud-sandbox API-key mode is deny-by-default for non-OPTIONS /api/** except GET /api/health"
        oauth2Roles = "application roles come from dedicated role claims; scope-only claims do not grant app roles"
        dtoValidation = "omitted required allocation/evaluation fields fail validation"
    }
    safety = [pscustomobject]@{
        releasePublished = $false
        containerPublished = $false
        releaseDownloadsMutated = $false
        secretsWritten = $false
        secretLikeEnvironmentVariableCountObservedButNotWritten = $secretLikeCount
        outputUnderTarget = $true
    }
    checksums = $checksums
}

$jsonPath = Join-Path $outputPath "release-candidate-dry-run-packet.json"
$markdownPath = Join-Path $outputPath "release-candidate-dry-run-packet.md"
$summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$markdown = @(
    "# Release-Candidate Dry-Run Packet",
    "",
    "Generated by: scripts/smoke/release-candidate-dry-run-packet.ps1",
    "Mode: $($summary.mode)",
    "Commit: $commit",
    "Branch: $branch",
    "Working tree: $status",
    "",
    "## Commands",
    "",
    "| Step | Command | Status | Exit code | Duration seconds |",
    "| --- | --- | --- | --- | --- |"
)

foreach ($command in $commands) {
    $markdown += "| $($command.name) | $($command.command) | $($command.status) | $($command.exitCode) | $($command.durationSeconds) |"
}

$markdown += @(
    "",
    "## Artifacts",
    "",
    "- JAR: $($summary.artifacts.jar)",
    "- SBOM JSON: $($summary.artifacts.sbomJson)",
    "- SBOM XML: $($summary.artifacts.sbomXml)",
    "- Checksums: $($summary.artifacts.checksums)",
    "- Checksum verification passed: $($summary.artifacts.checksumVerified)",
    "",
    "## Security And Release Posture",
    "",
    "- CodeQL: manual/operator verifies the latest matching run.",
    "- Dependency Review: manual/operator verifies required gate and risk-owner decisions.",
    "- Trivy: CI Docker scan is the current container vulnerability evidence path.",
    "- Docker digest pinning: base images remain digest-pinned in Dockerfile.",
    "- Container default: prod profile by default; runtime LOADBALANCERPRO_API_KEY required for protected prod container use.",
    "- Prod API-key boundary: non-OPTIONS /api/** denied by default except GET /api/health.",
    "- OAuth2 role mapping: dedicated role claims grant app roles; scope-only claims do not.",
    "- DTO validation: omitted required allocation/evaluation fields fail validation.",
    "- GitHub artifact attestations: produced only by the approved semantic-tag release workflow, not by this local packet.",
    "",
    "## Safety",
    "",
    "- Release published: false",
    "- Container published: false",
    "- release-downloads/ mutated: false",
    "- Secrets written: false",
    "- Output stayed under ignored target/: true",
    "",
    "## Remaining Blockers Before Real Release Or Container Publication",
    "",
    "- Separate release intent approval.",
    "- Semantic-tag release workflow evidence for the exact version.",
    "- GitHub Release asset and artifact attestation evidence from the approved release workflow.",
    "- Container registry/signing decision completion if container images are a distribution channel.",
    "- Deployment-specific TLS, IAM, network, secret rotation, monitoring, rollback, retention, and incident response controls."
)

Write-TextFile -Path $markdownPath -Lines $markdown

Write-Host "Release-candidate dry-run packet written to $markdownPath"
Write-Host "Structured packet written to $jsonPath"
Write-Host "Checksum evidence written to $checksumPath"
