param(
    [switch]$ExpectedOnly
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath "pom.xml" -PathType Leaf)) {
    throw "pom.xml not found; run the executable-JAR resolver from the repository root."
}

$mavenOutput = @(
    & mvn -q '-DforceStdout' '-Dstyle.color=never' help:evaluate '-Dexpression=project.build.finalName'
)
if ($LASTEXITCODE -ne 0) {
    throw "Maven could not resolve project.build.finalName."
}

$finalName = [string]($mavenOutput | Select-Object -Last 1)
$finalName = $finalName.Trim()
if ([string]::IsNullOrWhiteSpace($finalName) -or $finalName -notmatch '^[A-Za-z0-9._+-]+$') {
    throw "Maven returned an invalid project.build.finalName: $finalName"
}

$jarPath = "target/$finalName.jar"
if (-not $ExpectedOnly -and -not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
    throw "Expected executable jar not found: $jarPath. Run mvn -B -DskipTests package first."
}

Write-Output $jarPath
