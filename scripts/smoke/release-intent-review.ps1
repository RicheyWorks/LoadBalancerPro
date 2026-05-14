param(
    [switch]$DryRun,
    [string]$RecommendedVersion = "",
    [string]$OutputDir = "target/release-intent-review"
)

$ErrorActionPreference = "Stop"

if (-not $DryRun) {
    $DryRun = $true
}

function Invoke-GitRead {
    param([string[]]$Arguments)

    $output = & git @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        throw "git $($Arguments -join ' ') exited with code $exitCode"
    }
    return @($output)
}

function Assert-SafeOutputDirectory {
    param(
        [string]$RepoRoot,
        [string]$Candidate
    )

    $targetRoot = [System.IO.Path]::GetFullPath((Join-Path $RepoRoot "target"))
    $full = [System.IO.Path]::GetFullPath((Join-Path $RepoRoot $Candidate))
    if (-not $full.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Release-intent evidence output must stay under target/. Requested: $full"
    }
    return $full
}

function Read-ProjectVersion {
    [xml]$pom = Get-Content -LiteralPath "pom.xml"
    $version = [string]$pom.project.version
    if ([string]::IsNullOrWhiteSpace($version)) {
        throw "Could not read project version from pom.xml"
    }
    return $version.Trim()
}

function Get-LatestSemanticRef {
    $refs = Invoke-GitRead @("for-each-ref", "--sort=-v:refname", "--format=%(refname:short)", "refs/tags")
    foreach ($ref in $refs) {
        $value = ([string]$ref).Trim()
        if ($value -match "^v\d+\.\d+\.\d+$") {
            return $value
        }
    }
    return ""
}

function Get-NextMinorVersion {
    param([string]$Version)

    if ($Version -notmatch "^(\d+)\.(\d+)\.(\d+)$") {
        throw "Project version must be semantic, got: $Version"
    }
    $major = [int]$Matches[1]
    $minor = [int]$Matches[2] + 1
    return "$major.$minor.0"
}

function Assert-SemanticVersion {
    param(
        [string]$Name,
        [string]$Value
    )

    if ($Value -notmatch "^\d+\.\d+\.\d+$") {
        throw "$Name must be semantic without a leading v. Got: $Value"
    }
}

function Assert-NoSecretValue {
    param(
        [string]$Name,
        [string]$Value
    )

    $secretPatterns = @(
        "gh" + "p_",
        "github" + "_pat_",
        "-----" + "BEGIN",
        "client" + "_secret",
        "bearer\s+[a-z0-9._-]{20,}",
        "token="
    )
    foreach ($pattern in $secretPatterns) {
        if ($Value -match "(?i)$pattern") {
            throw "$Name looks like it may contain a secret value; refusing to write release-intent evidence."
        }
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
    return ""
}

function Find-FirstFile {
    param([string]$Pattern)

    $file = Get-ChildItem -Path "target" -Recurse -File -Filter $Pattern -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($file) {
        return $file.FullName
    }
    return ""
}

if ($env:LOADBALANCERPRO_RELEASE_PUBLISH -eq "true" -or $env:LOADBALANCERPRO_CONTAINER_PUBLISH -eq "true") {
    throw "Publishing flags are enabled; release-intent review must remain review-only."
}

$repoRoot = (Resolve-Path ".").Path
$safeOutput = Assert-SafeOutputDirectory -RepoRoot $repoRoot -Candidate $OutputDir
New-Item -ItemType Directory -Force -Path $safeOutput | Out-Null

$commit = [string](Invoke-GitRead @("rev-parse", "HEAD") | Select-Object -First 1)
$commit = $commit.Trim()
$branch = [string](Invoke-GitRead @("branch", "--show-current") | Select-Object -First 1)
$branch = $branch.Trim()
$statusLines = Invoke-GitRead @("status", "--short")
$workingTree = if ($statusLines.Count -eq 0) { "clean" } else { "dirty" }
$projectVersion = Read-ProjectVersion
$latestRef = Get-LatestSemanticRef
$resolvedRecommendation = if ([string]::IsNullOrWhiteSpace($RecommendedVersion)) {
    Get-NextMinorVersion -Version $projectVersion
} else {
    $RecommendedVersion.TrimStart("v")
}

Assert-SemanticVersion -Name "recommended version" -Value $resolvedRecommendation
foreach ($field in @($commit, $branch, $projectVersion, $latestRef, $resolvedRecommendation, $safeOutput)) {
    Assert-NoSecretValue -Name "release-intent field" -Value $field
}

$shortCommit = $commit.Substring(0, [Math]::Min(12, $commit.Length))
$jarPath = Find-ExecutableJar
$sbomJson = Find-FirstFile "LoadBalancerPro-*-bom.json"
$sbomXml = Find-FirstFile "LoadBalancerPro-*-bom.xml"
$checksums = Find-FirstFile "LoadBalancerPro-*-SHA256SUMS.txt"
$candidatePacket = Join-Path $repoRoot "target/release-candidate-dry-run/release-candidate-dry-run-packet.md"

$packet = [ordered]@{
    generatedAt = (Get-Date).ToString("o")
    mode = "review-only"
    commit = $commit
    shortCommit = $shortCommit
    branch = $branch
    workingTree = $workingTree
    currentProjectVersion = $projectVersion
    latestSemanticRef = $latestRef
    recommendedVersion = $resolvedRecommendation
    releaseType = "JAR/docs-first minor release"
    releaseTag = "v$resolvedRecommendation"
    versionAlignmentRequired = ($projectVersion -ne $resolvedRecommendation)
    releaseCandidatePacket = $candidatePacket
    executableJar = $jarPath
    sbomJson = $sbomJson
    sbomXml = $sbomXml
    checksums = $checksums
    publication = [ordered]@{
        semanticRefCreated = $false
        githubReleaseCreated = $false
        releaseAssetsUploaded = $false
        containerPublished = $false
        containerSigned = $false
        releaseDownloadsMutated = $false
        secretsWritten = $false
    }
}

$markdown = @(
    "# Release Intent Review Packet",
    "",
    "Generated by: scripts/smoke/release-intent-review.ps1",
    "Mode: review-only",
    "Commit: $commit",
    "Branch: $branch",
    "Working tree: $workingTree",
    "Current project version: $projectVersion",
    "Latest semantic release ref: $latestRef",
    "Recommended exact version: v$resolvedRecommendation",
    "Release type: JAR/docs-first minor release",
    "",
    "## Version Alignment",
    "",
    "- Version alignment required before release action: $($packet.versionAlignmentRequired)",
    "- Expected Maven/runtime metadata version before approval: $resolvedRecommendation",
    "- Expected release artifacts after approval: LoadBalancerPro-$resolvedRecommendation.jar, LoadBalancerPro-$resolvedRecommendation-bom.json, LoadBalancerPro-$resolvedRecommendation-bom.xml, LoadBalancerPro-$resolvedRecommendation-SHA256SUMS.txt",
    "",
    "## Evidence References",
    "",
    "- Release-candidate dry-run packet: $candidatePacket",
    "- Executable JAR found locally: $jarPath",
    "- SBOM JSON found locally: $sbomJson",
    "- SBOM XML found locally: $sbomXml",
    "- Checksum evidence found locally: $checksums",
    "- Review doc: docs/RELEASE_INTENT_REVIEW.md",
    "",
    "## Safety Boundary",
    "",
    "- Semantic version ref created: false",
    "- GitHub Release created: false",
    "- Release assets uploaded: false",
    "- Container published: false",
    "- Container signed: false",
    "- release-downloads/ mutated: false",
    "- Secrets written: false",
    "- Output stayed under ignored target/: true",
    "",
    "## Human Decision Required",
    "",
    "Approve a real release only in a separate request after version alignment, CI, CodeQL, Dependency Review, Trivy, SBOM, checksum, release-candidate packet, and release-intent packet evidence are reviewed."
)

$mdPath = Join-Path $safeOutput "release-intent-review.md"
$jsonPath = Join-Path $safeOutput "release-intent-review.json"
$markdown | Set-Content -LiteralPath $mdPath -Encoding UTF8
$packet | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

Write-Host "Release-intent review packet written to $mdPath"
Write-Host "Structured release-intent review written to $jsonPath"
