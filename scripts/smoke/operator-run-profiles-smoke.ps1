param(
    [switch]$DryRun,
    [switch]$Package,
    [switch]$SkipPackage,
    [int]$LocalPort = 18080,
    [int]$ApiKeyPort = 18081,
    [int]$ProxyPort = 18082,
    [int]$BackendAPort = 18181,
    [int]$BackendBPort = 18182,
    [string]$ApiKey = "CHANGE_ME_LOCAL_API_KEY"
)

$ErrorActionPreference = "Stop"
$script:SmokeProcesses = @()
$script:SmokeJobs = @()
$script:SmokeLogs = @()

function Write-SmokePass {
    param([string]$Message)
    Write-Host "PASS: $Message"
}

function Write-SmokeWarn {
    param([string]$Message)
    Write-Host "WARN: $Message"
}

function Write-SmokeFail {
    param([string]$Message)
    Write-Host "FAIL: $Message"
}

function Assert-PathExists {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "Required smoke-kit file is missing: $Path"
    }
    Write-SmokePass "found $Path"
}

function Find-ExecutableJar {
    $jar = Get-ChildItem -Path "target" -Filter "LoadBalancerPro-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "(-sources|-javadoc|-tests|\.original)\.jar$" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($jar) {
        return $jar.FullName
    }
    return $null
}

function Invoke-CheckedCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    Write-Host "Running: $FilePath $($Arguments -join ' ')"
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FilePath exited with code $LASTEXITCODE"
    }
}

function Start-SmokeApp {
    param(
        [string]$Name,
        [string[]]$Arguments
    )

    $stdout = Join-Path ([System.IO.Path]::GetTempPath()) "loadbalancerpro-$Name-$([Guid]::NewGuid()).out.log"
    $stderr = Join-Path ([System.IO.Path]::GetTempPath()) "loadbalancerpro-$Name-$([Guid]::NewGuid()).err.log"
    $script:SmokeLogs += $stdout
    $script:SmokeLogs += $stderr

    Write-Host "Starting $Name on loopback: java $($Arguments -join ' ')"
    $process = Start-Process -FilePath "java" -ArgumentList $Arguments -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    $script:SmokeProcesses += $process
    return $process
}

function Stop-SmokeApp {
    param(
        [System.Diagnostics.Process]$Process,
        [string]$Name
    )

    if ($Process -and -not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force
        Write-SmokePass "stopped $Name process"
    }
}

function Stop-SmokeResources {
    foreach ($process in $script:SmokeProcesses) {
        if ($process -and -not $process.HasExited) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        }
    }
    foreach ($job in $script:SmokeJobs) {
        Stop-Job -Job $job -ErrorAction SilentlyContinue
        Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-Http {
    param(
        [string]$Url,
        [hashtable]$Headers = @{}
    )

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -Headers $Headers -TimeoutSec 3
        return @{
            Status = [int]$response.StatusCode
            Body = [string]$response.Content
            Headers = $response.Headers
        }
    } catch {
        $response = $_.Exception.Response
        if ($response) {
            $statusCode = [int]$response.StatusCode
            return @{
                Status = $statusCode
                Body = ""
                Headers = @{}
            }
        }
        throw
    }
}

function Wait-ForStatus {
    param(
        [string]$Url,
        [int]$ExpectedStatus = 200,
        [int]$Attempts = 45,
        [hashtable]$Headers = @{}
    )

    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        $result = Invoke-Http -Url $Url -Headers $Headers
        if ($result.Status -eq $ExpectedStatus) {
            Write-SmokePass "$Url returned HTTP $ExpectedStatus"
            return $result
        }
        Start-Sleep -Seconds 1
    }
    throw "Timed out waiting for $Url to return HTTP $ExpectedStatus"
}

function Assert-HttpStatus {
    param(
        [string]$Url,
        [int]$ExpectedStatus,
        [hashtable]$Headers = @{}
    )

    $result = Invoke-Http -Url $Url -Headers $Headers
    if ($result.Status -ne $ExpectedStatus) {
        throw "Expected $Url to return HTTP $ExpectedStatus, got HTTP $($result.Status)"
    }
    Write-SmokePass "$Url returned HTTP $ExpectedStatus"
    return $result
}

function Start-LoopbackBackend {
    param(
        [string]$Name,
        [int]$Port
    )

    $job = Start-Job -Name "loadbalancerpro-smoke-$Name-$Port" -ArgumentList $Name, $Port -ScriptBlock {
        param($Name, $Port)

        $listener = [System.Net.HttpListener]::new()
        $listener.Prefixes.Add("http://127.0.0.1:$Port/")
        $listener.Start()
        try {
            while ($listener.IsListening) {
                $context = $listener.GetContext()
                $pathAndQuery = $context.Request.Url.PathAndQuery
                if ($context.Request.Url.AbsolutePath -eq "/health") {
                    $body = "$Name health ok"
                } else {
                    $body = "$Name handled $($context.Request.HttpMethod) $pathAndQuery"
                }
                $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
                $context.Response.StatusCode = 200
                $context.Response.ContentType = "text/plain; charset=utf-8"
                $context.Response.Headers.Set("X-Smoke-Backend", $Name)
                $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
                $context.Response.Close()
            }
        } finally {
            $listener.Close()
        }
    }

    $script:SmokeJobs += $job
    return $job
}

function Invoke-DryRun {
    $requiredPaths = @(
        "pom.xml",
        "Dockerfile",
        "docs/OPERATOR_RUN_PROFILES.md",
        "docs/examples/operator-run-profiles/local-demo.properties",
        "docs/examples/operator-run-profiles/prod-api-key.properties",
        "docs/examples/operator-run-profiles/cloud-sandbox-api-key.properties",
        "docs/examples/operator-run-profiles/proxy-loopback.properties",
        "src/main/resources/application.properties",
        "src/main/resources/static/index.html",
        "src/main/resources/static/load-balancing-cockpit.html",
        "src/main/resources/static/routing-demo.html"
    )

    foreach ($path in $requiredPaths) {
        Assert-PathExists -Path $path
    }

    Write-SmokeWarn "local Maven may be blocked by workstation PKIX trust-chain issues; GitHub CI remains the source of truth"
    Write-Host ""
    Write-Host "Package command:"
    Write-Host "  mvn -B -DskipTests package"
    Write-Host ""
    Write-Host "Live smoke command:"
    Write-Host "  pwsh ./scripts/smoke/operator-run-profiles-smoke.ps1 -Package"
    Write-Host ""
    Write-Host "Checks covered by live mode:"
    Write-Host "  local demo /api/health and root landing page"
    Write-Host "  prod API-key HTTP 401 without X-API-Key"
    Write-Host "  prod API-key HTTP 200 with X-API-Key"
    Write-Host "  proxy-loopback forwarding to 127.0.0.1:$BackendAPort and 127.0.0.1:$BackendBPort"
    Write-SmokePass "dry-run completed without external network, cloud credentials, or release actions"
}

function Invoke-LiveSmoke {
    $jarPath = Find-ExecutableJar
    if ($Package -or (-not $jarPath -and -not $SkipPackage)) {
        Invoke-CheckedCommand -FilePath "mvn" -Arguments @("-B", "-DskipTests", "package")
        $jarPath = Find-ExecutableJar
    }
    if (-not $jarPath) {
        throw "Executable jar not found under target/. Run with -Package or run mvn -B -DskipTests package first."
    }
    Write-SmokePass "using packaged jar $jarPath"

    $localApp = Start-SmokeApp -Name "local-demo" -Arguments @(
        "-jar", $jarPath,
        "--server.address=127.0.0.1",
        "--server.port=$LocalPort",
        "--spring.profiles.active=local"
    )
    try {
        Wait-ForStatus -Url "http://127.0.0.1:$LocalPort/api/health" | Out-Null
        Wait-ForStatus -Url "http://127.0.0.1:$LocalPort/" | Out-Null
    } finally {
        Stop-SmokeApp -Process $localApp -Name "local-demo"
    }

    $apiKeyApp = Start-SmokeApp -Name "prod-api-key" -Arguments @(
        "-jar", $jarPath,
        "--server.address=127.0.0.1",
        "--server.port=$ApiKeyPort",
        "--spring.profiles.active=prod",
        "--loadbalancerpro.api.key=$ApiKey"
    )
    try {
        Wait-ForStatus -Url "http://127.0.0.1:$ApiKeyPort/api/health" | Out-Null
        Assert-HttpStatus -Url "http://127.0.0.1:$ApiKeyPort/api/proxy/status" -ExpectedStatus 401 | Out-Null
        Assert-HttpStatus -Url "http://127.0.0.1:$ApiKeyPort/api/proxy/status" -ExpectedStatus 200 `
            -Headers @{ "X-API-Key" = $ApiKey } | Out-Null
    } finally {
        Stop-SmokeApp -Process $apiKeyApp -Name "prod-api-key"
    }

    Start-LoopbackBackend -Name "local-a" -Port $BackendAPort | Out-Null
    Start-LoopbackBackend -Name "local-b" -Port $BackendBPort | Out-Null
    Wait-ForStatus -Url "http://127.0.0.1:$BackendAPort/health" | Out-Null
    Wait-ForStatus -Url "http://127.0.0.1:$BackendBPort/health" | Out-Null

    $proxyApp = Start-SmokeApp -Name "proxy-loopback" -Arguments @(
        "-jar", $jarPath,
        "--server.address=127.0.0.1",
        "--server.port=$ProxyPort",
        "--spring.config.import=optional:file:docs/examples/operator-run-profiles/proxy-loopback.properties",
        "--loadbalancerpro.proxy.routes.api.targets[0].url=http://127.0.0.1:$BackendAPort",
        "--loadbalancerpro.proxy.routes.api.targets[1].url=http://127.0.0.1:$BackendBPort"
    )
    try {
        Wait-ForStatus -Url "http://127.0.0.1:$ProxyPort/api/health" | Out-Null
        $proxyResult = Assert-HttpStatus -Url "http://127.0.0.1:$ProxyPort/proxy/api/smoke?step=1" -ExpectedStatus 200
        if ($proxyResult.Body -notmatch "local-[ab] handled") {
            throw "Proxy-loopback response did not include the expected loopback backend body."
        }
        Write-SmokePass "proxy-loopback request reached a local backend"
        Assert-HttpStatus -Url "http://127.0.0.1:$ProxyPort/api/proxy/status" -ExpectedStatus 200 | Out-Null
    } finally {
        Stop-SmokeApp -Process $proxyApp -Name "proxy-loopback"
    }

    Write-SmokePass "operator run profiles live smoke completed"
}

$exitCode = 0
try {
    if ($DryRun) {
        Invoke-DryRun
    } else {
        Invoke-LiveSmoke
    }
} catch {
    Write-SmokeFail $_.Exception.Message
    if ($script:SmokeLogs.Count -gt 0) {
        Write-SmokeWarn "app logs were written under $([System.IO.Path]::GetTempPath())"
    }
    $exitCode = 1
} finally {
    Stop-SmokeResources
}
exit $exitCode
