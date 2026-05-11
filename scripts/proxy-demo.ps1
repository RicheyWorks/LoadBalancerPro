param(
    [int]$BackendAPort = 18081,
    [int]$BackendBPort = 18082
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
    Write-Host "Start LoadBalancerPro with proxy health checks in a second terminal:"
    Write-Host "mvn spring-boot:run `"-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=8080 --loadbalancerpro.proxy.enabled=true --loadbalancerpro.proxy.strategy=ROUND_ROBIN --loadbalancerpro.proxy.health-check.enabled=true --loadbalancerpro.proxy.health-check.interval=1s --loadbalancerpro.proxy.upstreams[0].id=backend-a --loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:$BackendAPort --loadbalancerpro.proxy.upstreams[1].id=backend-b --loadbalancerpro.proxy.upstreams[1].url=http://127.0.0.1:$BackendBPort`""
    Write-Host ""
    Write-Host "Optional retry/cooldown arguments for a resilience demo:"
    Write-Host "  --loadbalancerpro.proxy.retry.enabled=true"
    Write-Host "  --loadbalancerpro.proxy.retry.max-attempts=2"
    Write-Host "  --loadbalancerpro.proxy.cooldown.enabled=true"
    Write-Host "  --loadbalancerpro.proxy.cooldown.consecutive-failure-threshold=1"
    Write-Host "  --loadbalancerpro.proxy.cooldown.duration=30s"
    Write-Host ""
    Write-Host "Try:"
    Write-Host "  curl -i http://127.0.0.1:8080/proxy/demo"
    Write-Host "  curl -s http://127.0.0.1:8080/api/proxy/status"
    Write-Host "  curl http://127.0.0.1:$BackendBPort/fixture/health/fail"
    Write-Host "  curl -i http://127.0.0.1:8080/proxy/demo"
    Write-Host "  curl http://127.0.0.1:$BackendBPort/fixture/health/ok"
    Write-Host "  curl -s http://127.0.0.1:8080/api/proxy/status"
    Write-Host ""
    Read-Host "Press Enter to stop demo backends"
} finally {
    $jobs | Stop-Job -ErrorAction SilentlyContinue
    $jobs | Remove-Job -Force -ErrorAction SilentlyContinue
}
