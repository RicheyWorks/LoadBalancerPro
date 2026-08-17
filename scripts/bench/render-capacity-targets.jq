. as $profile
| [$profile.workload.routeMix[] as $route | range(0; $route.percent) | $route]
| to_entries[]
| . as $entry
| (if $entry.key < 50 then $profile.workload.payload.requestBytes.p50
   elif $entry.key < 95 then $profile.workload.payload.requestBytes.p95
   else $profile.workload.payload.requestBytes.p99 end) as $requestBytes
| (if $entry.key < 50 then $profile.workload.payload.responseBytes.p50
   elif $entry.key < 95 then $profile.workload.payload.responseBytes.p95
   else $profile.workload.payload.responseBytes.p99 end) as $responseBytes
| {
    method: $entry.value.method,
    url: ($baseUrl + $entry.value.path
      + (if ($entry.value.path | contains("?")) then "&" else "?" end)
      + "lbpResponseBytes=" + ($responseBytes | tostring)),
    header: {"X-API-Key": [$apiKey]}
  }
  + (if $requestBytes > 0
     then {body: (("x" * $requestBytes) | @base64)}
     else {} end)
