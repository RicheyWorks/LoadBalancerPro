param(
    [int]$BackendAPort = 18081,
    [int]$BackendBPort = 18082,
    [ValidateSet("round-robin", "weighted-round-robin", "failover")]
    [string]$Mode = "round-robin"
)

$ErrorActionPreference = "Stop"

function Start-DemoBackend {
    param(
        [string]$Name,
        [int]$Port
    )

    Start-Job -Name "loadbalancerpro-$Name" -ArgumentList $Name, $Port -ScriptBlock {
        param($Name, $Port)

        $healthy = $true
        $listener = [System.Net.HttpListener]::new()
        $listener.Prefixes.Add("http://127.0.0.1:$Port/")
        $listener.Start()

        try {
            while ($listener.IsListening) {
                $context = $listener.GetContext()
                $path = $context.Request.Url.AbsolutePath

                if ($path -eq "/fixture/health/fail") {
                    $healthy = $false
                    $statusCode = 200
                    $body = "$Name fixture health set to failing"
                } elseif ($path -eq "/fixture/health/ok") {
                    $healthy = $true
                    $statusCode = 200
                    $body = "$Name fixture health set to healthy"
                } elseif ($path -eq "/health") {
                    $statusCode = if ($healthy) { 200 } else { 503 }
                    $body = "$Name health=$healthy"
                } else {
                    $statusCode = 200
                    $body = "$Name handled $($context.Request.HttpMethod) $($context.Request.Url.PathAndQuery)"
                }

                $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
                $context.Response.StatusCode = $statusCode
                $context.Response.ContentType = "text/plain; charset=utf-8"
                $context.Response.Headers.Set("X-Fixture-Upstream", $Name)
                $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
                $context.Response.Close()
            }
        } finally {
            $listener.Close()
        }
    }
}

function Get-ProxyArguments {
    param(
        [string]$SelectedMode,
        [int]$BackendAPort,
        [int]$BackendBPort
    )

    $strategy = if ($SelectedMode -eq "weighted-round-robin") { "WEIGHTED_ROUND_ROBIN" } else { "ROUND_ROBIN" }
    $arguments = @(
        "--server.address=127.0.0.1",
        "--server.port=8080",
        "--loadbalancerpro.proxy.enabled=true",
        "--loadbalancerpro.proxy.strategy=$strategy",
        "--loadbalancerpro.proxy.health-check.enabled=true",
        "--loadbalancerpro.proxy.health-check.interval=1s",
        "--loadbalancerpro.proxy.upstreams[0].id=backend-a",
        "--loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:$BackendAPort",
        "--loadbalancerpro.proxy.upstreams[0].healthy=true",
        "--loadbalancerpro.proxy.upstreams[1].id=backend-b",
        "--loadbalancerpro.proxy.upstreams[1].url=http://127.0.0.1:$BackendBPort",
        "--loadbalancerpro.proxy.upstreams[1].healthy=true"
    )

    if ($SelectedMode -eq "weighted-round-robin") {
        $arguments += @(
            "--loadbalancerpro.proxy.upstreams[0].weight=3.0",
            "--loadbalancerpro.proxy.upstreams[1].weight=1.0"
        )
    }

    return $arguments -join " "
}

function Write-DemoCurlRecipes {
    param(
        [string]$SelectedMode,
        [int]$BackendBPort
    )

    Write-Host "Try:"
    if ($SelectedMode -eq "weighted-round-robin") {
        Write-Host "  # Expected first four selected upstreams with weights 3:1: backend-a, backend-a, backend-b, backend-a"
        Write-Host "  curl -i http://127.0.0.1:8080/proxy/weighted?step=1"
        Write-Host "  curl -i http://127.0.0.1:8080/proxy/weighted?step=2"
        Write-Host "  curl -i http://127.0.0.1:8080/proxy/weighted?step=3"
        Write-Host "  curl -i http://127.0.0.1:8080/proxy/weighted?step=4"
    } elseif ($SelectedMode -eq "failover") {
        Write-Host "  curl http://127.0.0.1:$BackendBPort/fixture/health/fail"
        Write-Host "  curl -i http://127.0.0.1:8080/proxy/failover?step=1"
        Write-Host "  curl -s http://127.0.0.1:8080/api/proxy/status"
        Write-Host "  curl http://127.0.0.1:$BackendBPort/fixture/health/ok"
        Write-Host "  curl -i http://127.0.0.1:8080/proxy/failover?step=2"
    } else {
        Write-Host "  # Expected first four selected upstreams: backend-a, backend-b, backend-a, backend-b"
        Write-Host "  curl -i http://127.0.0.1:8080/proxy/demo?step=1"
        Write-Host "  curl -i http://127.0.0.1:8080/proxy/demo?step=2"
        Write-Host "  curl -i http://127.0.0.1:8080/proxy/demo?step=3"
        Write-Host "  curl -i http://127.0.0.1:8080/proxy/demo?step=4"
    }
    Write-Host "  curl -s http://127.0.0.1:8080/api/proxy/status"
    Write-Host "  Browser status page: http://localhost:8080/proxy-status.html"
}

$jobs = @(
    Start-DemoBackend -Name "backend-a" -Port $BackendAPort
    Start-DemoBackend -Name "backend-b" -Port $BackendBPort
)

try {
    Start-Sleep -Milliseconds 500
    Write-Host "Started local demo backends:"
    Write-Host "  backend-a http://127.0.0.1:$BackendAPort"
    Write-Host "  backend-b http://127.0.0.1:$BackendBPort"
    Write-Host ""
    Write-Host "Selected strategy demo mode: $Mode"
    Write-Host ""
    Write-Host "Start LoadBalancerPro in a second terminal:"
    $proxyArguments = Get-ProxyArguments -SelectedMode $Mode -BackendAPort $BackendAPort -BackendBPort $BackendBPort
    Write-Host "mvn spring-boot:run `"-Dspring-boot.run.arguments=$proxyArguments`""
    Write-Host ""
    Write-Host "Optional retry/cooldown arguments for a resilience demo:"
    Write-Host "  --loadbalancerpro.proxy.retry.enabled=true"
    Write-Host "  --loadbalancerpro.proxy.retry.max-attempts=2"
    Write-Host "  --loadbalancerpro.proxy.cooldown.enabled=true"
    Write-Host "  --loadbalancerpro.proxy.cooldown.consecutive-failure-threshold=1"
    Write-Host "  --loadbalancerpro.proxy.cooldown.duration=30s"
    Write-Host ""
    Write-DemoCurlRecipes -SelectedMode $Mode -BackendBPort $BackendBPort
    Write-Host ""
    Read-Host "Press Enter to stop demo backends"
} finally {
    $jobs | Stop-Job -ErrorAction SilentlyContinue
    $jobs | Remove-Job -Force -ErrorAction SilentlyContinue
}
