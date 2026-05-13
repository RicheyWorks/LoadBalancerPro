param(
    [switch]$DryRun,
    [switch]$Package,
    [int]$LocalPort = 18080,
    [int]$ProdPort = 18081,
    [string]$ApiKey = "CHANGE_ME_LOCAL_API_KEY",
    [string]$EvidenceDir
)

$ErrorActionPreference = "Stop"

$CollectionPath = "docs/postman/LoadBalancerPro Enterprise Lab.postman_collection.json"
$EnvironmentPath = "docs/postman/LoadBalancerPro Local.postman_environment.json"
$WrongApiKey = "WRONG_CHANGE_ME_LOCAL_API_KEY"
$StartedJobs = @()
$SmokeResults = @()

function Redact-Text {
    param([string]$Text)

    if ([string]::IsNullOrEmpty($Text)) {
        return $Text
    }

    return $Text.Replace($ApiKey, "<REDACTED>").Replace($WrongApiKey, "<REDACTED>")
}

function Assert-RequiredFile {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Required Postman smoke file is missing: $Path"
    }
}

function Find-ExecutableJar {
    $jar = Get-ChildItem -Path "target" -Filter "*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "sources|javadoc|original" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        Write-Host "No packaged jar found; running mvn package."
        & mvn -q -DskipTests package
        $jar = Get-ChildItem -Path "target" -Filter "*.jar" -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch "sources|javadoc|original" } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
    }

    if ($null -eq $jar) {
        throw "No packaged Spring Boot jar was found under target."
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

    for ($attempt = 1; $attempt -le 30; $attempt++) {
        try {
            $status = Invoke-SmokeRequest -Url $Url
            if ($status -ge 200 -and $status -lt 500) {
                return
            }
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }

    throw "Timed out waiting for local smoke endpoint: $Url"
}

function Start-OrReuseApp {
    param(
        [string]$Name,
        [int]$Port,
        [string]$Profile,
        [switch]$RequireApiKey
    )

    $healthUrl = "http://127.0.0.1:$Port/api/health"
    try {
        $status = Invoke-SmokeRequest -Url $healthUrl
        if ($status -eq 200) {
            Write-Host "$Name already reachable at $healthUrl"
            return $null
        }
    } catch {
        Write-Host "$Name will be started on $Port"
    }

    $jar = Find-ExecutableJar
    $job = Start-Job -Name "loadbalancerpro-postman-safe-smoke-$Name-$Port" -ArgumentList $jar, $Port, $Profile, $RequireApiKey.IsPresent, $ApiKey -ScriptBlock {
        param($JarPath, $AppPort, $AppProfile, $NeedsKey, $LocalApiKey)
        if ($NeedsKey) {
            $env:LOADBALANCERPRO_API_KEY = $LocalApiKey
        }
        & java -jar $JarPath "--server.port=$AppPort" "--spring.profiles.active=$AppProfile" "--loadbalancerpro.auth.mode=api-key"
    }

    $script:StartedJobs += $job
    Wait-ForLocalHttp -Url $healthUrl
    return $job
}

function Add-SmokeResult {
    param(
        [string]$Profile,
        [string]$Name,
        [string]$Url,
        [int]$Expected,
        [int]$Actual
    )

    $Passed = $Expected -eq $Actual
    $script:SmokeResults += [pscustomobject]@{
        profile = $Profile
        name = $Name
        url = $Url
        expectedStatus = $Expected
        actualStatus = $Actual
        passed = $Passed
        apiKey = "<REDACTED>"
    }

    if ($Passed) {
        Write-Host "PASS: $Name -> HTTP $Actual"
    } else {
        Write-Host "FAIL: $Name expected HTTP $Expected but got HTTP $Actual"
    }
}

function Assert-HttpStatus {
    param(
        [string]$Profile,
        [string]$Name,
        [string]$Url,
        [int]$Expected,
        [string]$Method = "GET",
        [string]$HeaderApiKey,
        [string]$Body
    )

    $actual = Invoke-SmokeRequest -Url $Url -Method $Method -HeaderApiKey $HeaderApiKey -Body $Body
    Add-SmokeResult -Profile $Profile -Name $Name -Url $Url -Expected $Expected -Actual $actual
    if ($actual -ne $Expected) {
        throw "$Name expected HTTP $Expected but got HTTP $actual"
    }
}

function Write-SmokeEvidence {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return
    }

    New-Item -ItemType Directory -Force -Path $Path | Out-Null

    $jsonPath = Join-Path $Path "postman-enterprise-lab-smoke.json"
    $markdownPath = Join-Path $Path "postman-enterprise-lab-smoke.md"
    $summary = [pscustomobject]@{
        generatedBy = "postman-enterprise-lab-safe-smoke.ps1"
        apiKey = "<REDACTED>"
        collection = $CollectionPath
        environment = $EnvironmentPath
        results = $SmokeResults
        safety = "No secrets, bearer tokens, cookies, credentials, cloud mutation, release action, or public endpoints."
    }

    $summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

    $lines = @(
        "# Postman Enterprise Lab Smoke",
        "",
        "Collection: $CollectionPath",
        "Environment: $EnvironmentPath",
        "API key: <REDACTED>",
        "",
        "No secrets, bearer tokens, cookies, credentials, cloud mutation, release action, or public endpoints.",
        "",
        "| Profile | Check | Expected | Actual | Passed |",
        "| --- | --- | --- | --- | --- |"
    )

    foreach ($result in $SmokeResults) {
        $lines += "| $($result.profile) | $($result.name) | $($result.expectedStatus) | $($result.actualStatus) | $($result.passed) |"
    }

    $lines | Set-Content -LiteralPath $markdownPath -Encoding UTF8
}

function Invoke-PostmanEnterpriseLabSmoke {
    Assert-RequiredFile -Path $CollectionPath
    Assert-RequiredFile -Path $EnvironmentPath

    if ($DryRun -or -not $Package) {
        Write-Host "Postman enterprise lab smoke dry run."
        Write-Host "Collection: $CollectionPath"
        Write-Host "Environment: $EnvironmentPath"
        Write-Host "Local profile port: $LocalPort"
        Write-Host "Prod API-key profile port: $ProdPort"
        Write-Host "API key: <REDACTED>"
        Write-Host "Live command: powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\postman-enterprise-lab-safe-smoke.ps1 -Package"
        return
    }

    Start-OrReuseApp -Name "local" -Port $LocalPort -Profile "local"
    Start-OrReuseApp -Name "prod" -Port $ProdPort -Profile "prod" -RequireApiKey

    $localBase = "http://127.0.0.1:$LocalPort"
    $prodBase = "http://127.0.0.1:$ProdPort"
    $routingBody = '{"strategy":"ROUND_ROBIN","requestId":"postman-smoke","servers":[{"id":"a","healthy":true,"activeConnections":0,"weight":1},{"id":"b","healthy":true,"activeConnections":0,"weight":1}]}'
    $evaluationBody = '{"totalLoad":10,"servers":[{"id":"a","capacity":10,"currentLoad":2,"healthy":true},{"id":"b","capacity":10,"currentLoad":2,"healthy":true}]}'

    Assert-HttpStatus -Profile "local" -Name "Root allowed" -Url "$localBase/" -Expected 200
    Assert-HttpStatus -Profile "local" -Name "Health allowed" -Url "$localBase/api/health" -Expected 200
    Assert-HttpStatus -Profile "local" -Name "Actuator health allowed" -Url "$localBase/actuator/health" -Expected 200
    Assert-HttpStatus -Profile "local" -Name "OpenAPI allowed" -Url "$localBase/v3/api-docs" -Expected 200
    Assert-HttpStatus -Profile "local" -Name "Swagger allowed" -Url "$localBase/swagger-ui/index.html" -Expected 200

    Assert-HttpStatus -Profile "prod" -Name "OpenAPI missing key gated" -Url "$prodBase/v3/api-docs" -Expected 401
    Assert-HttpStatus -Profile "prod" -Name "OpenAPI wrong key gated" -Url "$prodBase/v3/api-docs" -Expected 401 -HeaderApiKey $WrongApiKey
    Assert-HttpStatus -Profile "prod" -Name "OpenAPI correct key allowed" -Url "$prodBase/v3/api-docs" -Expected 200 -HeaderApiKey $ApiKey
    Assert-HttpStatus -Profile "prod" -Name "Swagger missing key gated" -Url "$prodBase/swagger-ui/index.html" -Expected 401
    Assert-HttpStatus -Profile "prod" -Name "routing missing key gated" -Url "$prodBase/api/routing/compare" -Expected 401 -Method "POST" -Body $routingBody
    Assert-HttpStatus -Profile "prod" -Name "routing wrong key gated" -Url "$prodBase/api/routing/compare" -Expected 401 -Method "POST" -HeaderApiKey $WrongApiKey -Body $routingBody
    Assert-HttpStatus -Profile "prod" -Name "routing correct key allowed" -Url "$prodBase/api/routing/compare" -Expected 200 -Method "POST" -HeaderApiKey $ApiKey -Body $routingBody
    Assert-HttpStatus -Profile "prod" -Name "evaluation missing key gated" -Url "$prodBase/api/allocate/evaluate" -Expected 401 -Method "POST" -Body $evaluationBody
    Assert-HttpStatus -Profile "prod" -Name "evaluation wrong key gated" -Url "$prodBase/api/allocate/evaluate" -Expected 401 -Method "POST" -HeaderApiKey $WrongApiKey -Body $evaluationBody
    Assert-HttpStatus -Profile "prod" -Name "evaluation correct key allowed" -Url "$prodBase/api/allocate/evaluate" -Expected 200 -Method "POST" -HeaderApiKey $ApiKey -Body $evaluationBody
    Assert-HttpStatus -Profile "prod" -Name "actuator metrics not public" -Url "$prodBase/actuator/metrics" -Expected 401
    Assert-HttpStatus -Profile "prod" -Name "actuator Prometheus not public" -Url "$prodBase/actuator/prometheus" -Expected 401

    Write-SmokeEvidence -Path $EvidenceDir
    Write-Host "Postman enterprise lab smoke completed."
}

try {
    Invoke-PostmanEnterpriseLabSmoke
} finally {
    foreach ($job in $StartedJobs) {
        Stop-Job -Job $job -ErrorAction SilentlyContinue
        Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
    }
}
