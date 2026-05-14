param(
    [switch]$DryRun,
    [switch]$Package,
    [string]$OutputDir = "target/adaptive-routing-experiments"
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
        throw "Adaptive-routing experiment output must stay under target/. Requested: $Path"
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
            throw "Refusing to write adaptive-routing experiment output that looks like it contains a secret."
        }
    }
}

$resolvedOutputDir = Assert-OutputUnderTarget -Path $OutputDir
$markdownPath = Join-Path $resolvedOutputDir "adaptive-routing-experiment.md"
$metadataPath = Join-Path $resolvedOutputDir "adaptive-routing-experiment-metadata.json"

if ($DryRun) {
    Write-Host "Adaptive-routing experiment dry run."
    Write-Host "Output directory: $OutputDir"
    Write-Host "Planned command: java -jar target/LoadBalancerPro-*.jar --adaptive-routing-experiment=all"
    Write-Host "Safety: local deterministic CLI only; no API server, cloud mutation, external network, release, tag, asset, container, or registry action."
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

$output = & java -jar $jarPath --adaptive-routing-experiment=all 2>&1
$exitCode = $LASTEXITCODE
$markdown = ($output -join "`n")
if ($exitCode -ne 0) {
    throw "Adaptive-routing experiment CLI failed with exit code $exitCode"
}
Assert-NoSecretValues -Text $markdown
Set-Content -LiteralPath $markdownPath -Value $markdown -Encoding UTF8

$metadata = [ordered]@{
    generatedAt = (Get-Date).ToUniversalTime().ToString("o")
    gitCommit = (& git rev-parse HEAD)
    jarPath = $jarPath
    markdownPath = $markdownPath
    mode = "all"
    safety = "local deterministic CLI only; no API server, cloud mutation, external network, release, tag, asset, container, or registry action"
}
Set-Content -LiteralPath $metadataPath -Value ($metadata | ConvertTo-Json -Depth 4) -Encoding UTF8

Write-Host "PASS: adaptive-routing experiment evidence written to $markdownPath"
Write-Host "PASS: metadata written to $metadataPath"
