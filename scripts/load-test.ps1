param(
    [string]$BaseUrl = "http://127.0.0.1:18080",
    [int]$SteadyConnections = 10,
    [string]$SteadyDuration = "30s",
    [int]$BurstConnections = 50,
    [int]$BurstRequests = 1000
)

$ErrorActionPreference = "Stop"

if ($BaseUrl -notmatch '^https?://(127\.0\.0\.1|localhost)(:\d+)?$') {
    throw "Refusing to load test non-local target: $BaseUrl"
}

if (-not (Get-Command hey -ErrorAction SilentlyContinue)) {
    throw "The 'hey' load-test tool is required. Install hey, start LoadBalancerPro locally, then rerun this script."
}

$allocationPayload = @'
{
  "requestedLoad": 75.0,
  "servers": [
    {
      "id": "api-1",
      "cpuUsage": 35.0,
      "memoryUsage": 40.0,
      "diskUsage": 30.0,
      "capacity": 100.0,
      "weight": 1.0,
      "healthy": true
    },
    {
      "id": "worker-1",
      "cpuUsage": 45.0,
      "memoryUsage": 35.0,
      "diskUsage": 25.0,
      "capacity": 120.0,
      "weight": 1.0,
      "healthy": true
    }
  ]
}
'@

$payloadFile = New-TemporaryFile
try {
    Set-Content -LiteralPath $payloadFile.FullName -Value $allocationPayload -NoNewline -Encoding UTF8

    Write-Host "== Health endpoint steady load =="
    hey -z $SteadyDuration -c $SteadyConnections "$BaseUrl/api/health"

    Write-Host "== Allocation endpoint steady load =="
    hey -z $SteadyDuration -c $SteadyConnections -m POST `
        -H "Content-Type: application/json" `
        -D $payloadFile.FullName `
        "$BaseUrl/api/allocate/capacity-aware"

    Write-Host "== Allocation endpoint burst/spike load =="
    hey -n $BurstRequests -c $BurstConnections -m POST `
        -H "Content-Type: application/json" `
        -D $payloadFile.FullName `
        "$BaseUrl/api/allocate/capacity-aware"
} finally {
    Remove-Item -LiteralPath $payloadFile.FullName -Force -ErrorAction SilentlyContinue
}
