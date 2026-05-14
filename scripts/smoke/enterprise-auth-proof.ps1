param(
    [switch]$DryRun,
    [switch]$Package,
    [int]$ProdPort = 19690,
    [string]$OutputDir = "target/enterprise-auth-proof",
    [string]$FixturePath = "src/test/resources/auth-proof/mock-idp-claims.json",
    [string]$ApiKey = "CHANGE_ME_LOCAL_API_KEY"
)

$ErrorActionPreference = "Stop"
$StartedJobs = @()
$ProofResults = @()
$WrongApiKey = "WRONG_CHANGE_ME_LOCAL_API_KEY"

function Resolve-RepoPath {
    param([string]$Path)
    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $Path))
}

function Assert-OutputUnderTarget {
    param([string]$Path)

    $targetRoot = Resolve-RepoPath "target"
    $resolvedOutput = Resolve-RepoPath $Path
    if (-not $resolvedOutput.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Enterprise auth proof output must stay under target/. Requested: $Path"
    }
    return $resolvedOutput
}

function Assert-NoSecretValues {
    param([string]$Text)

    $patterns = @(
        '(?i)bearer\s+[a-z0-9._~+/-]{12,}',
        '(?i)x-api-key\s*[:=]\s*[a-z0-9._~+/-]{8,}',
        '(?i)(password|secret|credential|token)\s*[:=]\s*[a-z0-9._~+/-]{8,}',
        '-----BEGIN [A-Z ]*PRIVATE KEY-----'
    )
    $sanitized = $Text.Replace($ApiKey, "<REDACTED>").Replace($WrongApiKey, "<REDACTED>")
    foreach ($pattern in $patterns) {
        if ($sanitized -match $pattern) {
            throw "Refusing to write enterprise auth proof evidence that looks like it contains a secret."
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

function Invoke-SmokeRequest {
    param(
        [string]$Url,
        [string]$Method = "GET",
        [string]$HeaderApiKey,
        [string]$Body
    )

    $request = @{
        Uri = $Url
        Method = $Method
        TimeoutSec = 5
        ErrorAction = "Stop"
        UseBasicParsing = $true
    }
    if ($HeaderApiKey) {
        $request["Headers"] = @{ "X-API-Key" = $HeaderApiKey }
    }
    if ($Body) {
        $request["ContentType"] = "application/json"
        $request["Body"] = $Body
    }
    try {
        $response = Invoke-WebRequest @request
        return [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

function Wait-ForLocalHttp {
    param([string]$Url)

    for ($attempt = 1; $attempt -le 90; $attempt++) {
        try {
            $status = Invoke-SmokeRequest -Url $Url
            if ($status -eq 200) {
                return
            }
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
    throw "Timed out waiting for local auth proof endpoint: $Url"
}

function Add-ProofResult {
    param(
        [string]$Name,
        [int]$Expected,
        [int]$Actual,
        [string]$Mode
    )

    $script:ProofResults += [pscustomobject]@{
        name = $Name
        mode = $Mode
        expectedStatus = $Expected
        actualStatus = $Actual
        passed = $Expected -eq $Actual
    }
    if ($Expected -ne $Actual) {
        throw "$Name expected HTTP $Expected but got HTTP $Actual"
    }
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

function Invoke-MavenTestCapture {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $output = & mvn -q "-Dtest=EnterpriseAuthProofLaneTest,OAuth2AuthorizationTest" test 2>&1
        return [pscustomobject]@{
            exitCode = $LASTEXITCODE
            text = ($output | ForEach-Object { $_.ToString() }) -join "`n"
        }
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

$resolvedOutputDir = Assert-OutputUnderTarget -Path $OutputDir
if (-not (Test-Path -LiteralPath $FixturePath)) {
    throw "Mock IdP/JWKS fixture file missing: $FixturePath"
}

if ($DryRun) {
    Write-Host "Enterprise auth proof dry run."
    Write-Host "Fixture: $FixturePath"
    Write-Host "Output directory: $OutputDir"
    Write-Host "Planned proof: prod-style API-key boundary smoke plus mocked OAuth2/JWKS role-claim tests."
    Write-Host "Safety: no real IdP, tenant, private key, secret, live network, release, tag, asset, container, or registry action."
    exit 0
}

try {
    $fixture = Get-Content -LiteralPath $FixturePath -Raw | ConvertFrom-Json
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

    $job = Start-Job -Name "loadbalancerpro-enterprise-auth-proof-$ProdPort" -ArgumentList $jarPath, $ProdPort, $ApiKey -ScriptBlock {
        param($JarPath, $AppPort, $LocalApiKey)
        $env:LOADBALANCERPRO_API_KEY = $LocalApiKey
        & java -jar $JarPath "--server.address=127.0.0.1" "--server.port=$AppPort" "--spring.profiles.active=prod" "--loadbalancerpro.auth.mode=api-key"
    }
    $StartedJobs += $job
    $base = "http://127.0.0.1:$ProdPort"
    Wait-ForLocalHttp -Url "$base/api/health"

    $missing = Invoke-SmokeRequest -Url "$base/api/lab/metrics"
    Add-ProofResult -Name "Prod-style protected lab metrics deny missing API key" -Expected 401 -Actual $missing -Mode "api-key"
    $wrong = Invoke-SmokeRequest -Url "$base/api/lab/metrics" -HeaderApiKey $WrongApiKey
    Add-ProofResult -Name "Prod-style protected lab metrics deny wrong API key" -Expected 401 -Actual $wrong -Mode "api-key"
    $allowed = Invoke-SmokeRequest -Url "$base/api/lab/metrics" -HeaderApiKey $ApiKey
    Add-ProofResult -Name "Prod-style protected lab metrics allow placeholder API key" -Expected 200 -Actual $allowed -Mode "api-key"

    $testResult = Invoke-MavenTestCapture
    $testExit = $testResult.exitCode
    $testText = $testResult.text
    Assert-NoSecretValues -Text $testText
    if ($testExit -ne 0) {
        throw "Enterprise auth proof tests failed with exit code $testExit"
    }

    $metadata = [ordered]@{
        generatedBy = "enterprise-auth-proof.ps1"
        generatedAtUtc = [DateTime]::UtcNow.ToString("o")
        commit = Git-Value -GitArgs @("rev-parse", "HEAD")
        branch = Git-Value -GitArgs @("branch", "--show-current")
        projectVersion = Project-Version
        fixtureVersion = $fixture.fixtureVersion
        issuer = $fixture.issuer
        audience = $fixture.audience
        jwksMode = $fixture.jwksMode
        scope = "local/test-backed proof lane; no real enterprise IdP tenant validation"
    }

    $results = [ordered]@{
        metadata = $metadata
        apiKeyBoundary = $ProofResults
        oauth2Proof = [ordered]@{
            command = "mvn -q `"-Dtest=EnterpriseAuthProofLaneTest,OAuth2AuthorizationTest`" test"
            passed = $true
            claimsProven = @(
                "dedicated roles claim grants operator route access",
                "realm_access.roles grants operator route access during key-rotation example",
                "scope-only operator/admin does not grant application role",
                "missing role claims fail closed",
                "ambiguous scope-authorities fail closed",
                "expired token is rejected",
                "wrong issuer is rejected",
                "wrong audience is rejected"
            )
            limitation = "Packaged OAuth2 proof is not exercised because no real or local IdP server is started; proof is mocked-resource-server and fixture backed."
        }
        safety = "No real tenant IDs, client secrets, private keys, production JWTs, live IdP calls, release action, or container publication."
    }
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "enterprise-auth-proof-results.json") `
        -Content ($results | ConvertTo-Json -Depth 12)

    $fixtureCopy = [ordered]@{
        fixtureVersion = $fixture.fixtureVersion
        issuer = $fixture.issuer
        audience = $fixture.audience
        jwksMode = $fixture.jwksMode
        keys = $fixture.keys
        tokenNames = @($fixture.tokens | ForEach-Object { $_.name })
        safety = $fixture.safety
    }
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "mock-idp-jwks-fixture-summary.json") `
        -Content ($fixtureCopy | ConvertTo-Json -Depth 8)

    $lines = @(
        "# Enterprise Auth Proof Evidence",
        "",
        "Status: pass",
        "Commit: $($metadata.commit)",
        "Project version: $($metadata.projectVersion)",
        "Scope: local/test-backed proof lane; no real enterprise IdP tenant validation.",
        "",
        "## API-Key Boundary",
        "",
        "| Check | Expected | Actual | Passed |",
        "| --- | ---: | ---: | --- |"
    )
    foreach ($result in $ProofResults) {
        $lines += "| $($result.name) | $($result.expectedStatus) | $($result.actualStatus) | $($result.passed) |"
    }
    $lines += ""
    $lines += "## OAuth2 Role-Claim Proof"
    $lines += ""
    $lines += "- Dedicated role claims grant configured app roles."
    $lines += "- `scope` and `scp` claims do not grant `ROLE_operator` or `ROLE_admin`."
    $lines += "- Missing and ambiguous role claims fail closed."
    $lines += "- Expired, wrong-issuer, and wrong-audience fixture tokens are rejected."
    $lines += '- Key rotation is represented by synthetic `kid` values `test-key-a` and `test-key-b`; no private key material is stored.'
    $lines += ""
    $lines += "Limitation: packaged OAuth2 proof is not exercised because this script does not start a real or local IdP server. The proof is mocked-resource-server and fixture backed."
    $lines += ""
    $lines += "Safety: no real tenant IDs, client secrets, private keys, production JWTs, live IdP calls, release action, or container publication."
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "enterprise-auth-proof-summary.md") `
        -Content ($lines -join "`n")

    $manifest = [ordered]@{
        generatedBy = "enterprise-auth-proof.ps1"
        outputDirectory = $resolvedOutputDir
        files = @(
            "enterprise-auth-proof-results.json",
            "mock-idp-jwks-fixture-summary.json",
            "enterprise-auth-proof-summary.md"
        )
        tests = "EnterpriseAuthProofLaneTest,OAuth2AuthorizationTest"
        safety = "No real IdP, tenant secrets, private keys, live network, release action, or container publication."
    } | ConvertTo-Json -Depth 6
    Write-EvidenceFile -Path (Join-Path $resolvedOutputDir "enterprise-auth-proof-manifest.json") -Content $manifest

    Write-Host "PASS: Enterprise auth proof evidence written under $OutputDir"
} finally {
    foreach ($job in $StartedJobs) {
        Stop-Job -Job $job -ErrorAction SilentlyContinue
        Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
    }
}
