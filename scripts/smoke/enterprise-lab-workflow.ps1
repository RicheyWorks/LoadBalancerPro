param(
    [switch]$DryRun,
    [switch]$Package,
    [string]$Mode = "all",
    [string]$OutputDir = "target/enterprise-lab-runs"
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
        throw "Enterprise Lab workflow output must stay under target/. Requested: $Path"
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
            throw "Refusing to write Enterprise Lab workflow output that looks like it contains a secret."
        }
    }
}

$resolvedOutputDir = Assert-OutputUnderTarget -Path $OutputDir

if ($DryRun) {
    Write-Host "Enterprise Lab workflow dry run."
    Write-Host "Output directory: $OutputDir"
    Write-Host "Planned command: java -jar target/LoadBalancerPro-*.jar --enterprise-lab-workflow=$Mode --enterprise-lab-output=$OutputDir"
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

$output = & java -jar $jarPath "--enterprise-lab-workflow=$Mode" "--enterprise-lab-output=$OutputDir" 2>&1
$exitCode = $LASTEXITCODE
$text = ($output -join "`n")
if ($exitCode -ne 0) {
    throw "Enterprise Lab workflow CLI failed with exit code $exitCode"
}
Assert-NoSecretValues -Text $text

$expectedFiles = @(
    "enterprise-lab-scenario-catalog.json",
    "enterprise-lab-run.json",
    "enterprise-lab-run-summary.md",
    "enterprise-lab-evidence-metadata.json"
)

foreach ($file in $expectedFiles) {
    $path = Join-Path $resolvedOutputDir $file
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Expected Enterprise Lab evidence file missing: $path"
    }
    Assert-NoSecretValues -Text (Get-Content -LiteralPath $path -Raw)
}

Write-Host $text
Write-Host "PASS: Enterprise Lab workflow evidence written under $OutputDir"

